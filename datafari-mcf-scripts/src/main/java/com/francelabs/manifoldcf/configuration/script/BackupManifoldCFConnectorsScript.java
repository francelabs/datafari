package com.francelabs.manifoldcf.configuration.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class BackupManifoldCFConnectorsScript {

  private final static Logger LOGGER = LogManager.getLogger(BackupManifoldCFConnectorsScript.class);

  private static boolean isAuthentified = false;

  private static final Set<String> restoreDirectoriesNames = new HashSet<>();
  static {
    restoreDirectoriesNames.add(ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);
    restoreDirectoriesNames.add(ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);
    restoreDirectoriesNames.add(ManifoldAPI.COMMANDS.AUTHORITYGROUPS);
    restoreDirectoriesNames.add(ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);
    restoreDirectoriesNames.add(ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);
    restoreDirectoriesNames.add(ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);
    restoreDirectoriesNames.add(ManifoldAPI.COMMANDS.JOBS);
  }

  /**
   * @param args
   * @throws Exception
   */
  public static void main(final String[] args) {

    if (args.length < 1) {
      LOGGER.fatal("No argument");
      return;
    }

    String backupDirectory;
    if (args.length < 2) {
      System.out.println("Backup directory : ");
      final Scanner input = new Scanner(System.in);
      backupDirectory = input.nextLine();
    } else {
      backupDirectory = args[1];
      if (args.length == 3 && args[2].equals("https")) {
        ManifoldAPI.useHttpsProtocol();
      }
    }

    try {
      final File outputConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);

      final File repositoryConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);

      final File authorityConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);

      final File mappingConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);

      final File transformationConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);

      final File authorityGroupsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYGROUPS);

      final File jobsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.JOBS);

      if (args[0].equals("BACKUP")) {

        prepareDirectory(outputConnectionsDir);
        saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS), outputConnectionsDir);

        prepareDirectory(repositoryConnectionsDir);
        saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS), repositoryConnectionsDir);

        prepareDirectory(authorityConnectionsDir);
        saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS), authorityConnectionsDir);

        prepareDirectory(mappingConnectionsDir);
        saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS), mappingConnectionsDir);

        prepareDirectory(transformationConnectionsDir);
        saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS), transformationConnectionsDir);

        prepareDirectory(authorityGroupsDir);
        saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.AUTHORITYGROUPS), authorityGroupsDir);

        prepareDirectory(jobsDir);
        saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.JOBS), jobsDir);

        LOGGER.info("Connectors Saved");

      }

      if (args[0].equals("RESTORE")) {
        ManifoldAPI.cleanAll();

        restoreAllConnections(outputConnectionsDir, ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);

        restoreAllConnections(transformationConnectionsDir, ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);

        restoreAllConnections(authorityGroupsDir, ManifoldAPI.COMMANDS.AUTHORITYGROUPS);

        restoreAllConnections(authorityConnectionsDir, ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);

        restoreAllConnections(repositoryConnectionsDir, ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);

        restoreAllConnections(mappingConnectionsDir, ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);

        restoreAllConnections(jobsDir, ManifoldAPI.COMMANDS.JOBS);

        LOGGER.info("Connectors Restored");
      }
      if (args[0].equals("RESTOREJOBS")) {

        restoreAllConnections(jobsDir, ManifoldAPI.COMMANDS.JOBS);

        LOGGER.info("Jobs Restored");
      }

      if (args[0].equals("STARTJOBS")) {
        LOGGER.info("Execution Start jobs");
        // takes in argument a list of job IDs : one per line
        final File f = new File(args[1]);
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
          String s;
          while ((s = br.readLine()) != null) {
            LOGGER.info("startJob");
            ManifoldAPI.startJob(s);
            ManifoldAPI.statusJob(s);
          }

        }
      }
      if (args[0].equals("DELETEJOBS")) {
        LOGGER.info("Execution Deletion jobs");
        // takes in argument a list of job IDs : one per line
        final File f = new File(args[1]);
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
          String s;
          while ((s = br.readLine()) != null) {
            ManifoldAPI.deleteJob(s);
          }

        }
      }
    } catch (final Exception e) {
      LOGGER.fatal(e.getMessage());
      e.printStackTrace();
    }
  }

  private static void prepareDirectory(final File directory) throws IOException {

    directory.mkdirs();
    final File[] files = directory.listFiles();
    for (final File file : files) {
      file.delete();
    }
  }

  private static void restoreAllConnections(final File directory, final String command) throws Exception {

    final File[] connectorFiles = directory.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(final File dir, final String name) {
        name.endsWith(".json");
        return true;
      }
    });
    for (final File connectorFile : connectorFiles) {
      restoreConnection(connectorFile, command);
    }

  }

  private static void restoreConnection(final File connectorFile, final String command) throws Exception {

    final JSONObject jsonObject = JSONUtils.readJSON(connectorFile);
    String name = connectorFile.getName();
    // Remove the .json from the file name
    name = name.substring(0, name.length() - 5);
    // URL encode the name as it is used in the HTTP API call
    name = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
    ManifoldAPI.putConfig(command, name, jsonObject);

  }

  private static void saveAllConnections(final Map<String, JSONObject> connections, final File directory) throws IOException {

    for (final Entry<String, JSONObject> connection : connections.entrySet()) {
      saveConnection(connection, directory);
    }
  }

  private static void saveConnection(final Entry<String, JSONObject> outputConnection, final File directory) throws IOException {

    JSONUtils.saveJSON(outputConnection.getValue(), new File(directory, outputConnection.getKey() + ".json"));
    final File connectorFile = new File(directory, outputConnection.getKey() + ".json");

  }

  /**
   * Method called by Servlet MCFBackupRestore
   */
  public static void doSave(final String backupDirectory) throws Exception {
    final File backupDirectoryFile = new File(backupDirectory);
    // check access right
    if (!backupDirectoryFile.canWrite()) {
      throw new IOException("Lack of permissions on directory : " + backupDirectory);
    }

    final File outputConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);

    final File authorityGroupsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYGROUPS);

    final File repositoryConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);

    final File authorityConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);

    final File mappingConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);

    final File transformationConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);

    final File jobsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.JOBS);

    try {

      prepareDirectory(outputConnectionsDir);
      saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS), outputConnectionsDir);

      prepareDirectory(repositoryConnectionsDir);
      saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS), repositoryConnectionsDir);

      prepareDirectory(authorityGroupsDir);
      saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.AUTHORITYGROUPS), authorityGroupsDir);

      prepareDirectory(authorityConnectionsDir);
      saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS), authorityConnectionsDir);

      prepareDirectory(mappingConnectionsDir);
      saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS), mappingConnectionsDir);

      prepareDirectory(transformationConnectionsDir);
      saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS), transformationConnectionsDir);

      prepareDirectory(jobsDir);
      saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.JOBS), jobsDir);

      LOGGER.info("Connectors Saved");

    } catch (final Exception e) {
      LOGGER.error("Error while saving MCF connections", e);
      throw new Exception("Error while saving MCF connections");
    }
  }

  /**
   * Method called by Servlet MCFBackupRestore
   */
  public static void doRestore(final String backupDirectory) throws Exception {
    final File backupDirectoryFile = new File(backupDirectory);
    // check access right
    if (!backupDirectoryFile.canRead()) {
      throw new IOException("Lack of permissions on directory : " + backupDirectory);
    }

    final File[] files = backupDirectoryFile.listFiles();
    if (checkRestoreFiles(files)) {

      final File outputConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);

      final File authorityGroupsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYGROUPS);

      final File transformationConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);

      final File mappingConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);

      final File authorityConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);

      final File repositoryConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);

      final File jobsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.JOBS);

      try {

        ManifoldAPI.cleanAll();

        restoreAllConnections(outputConnectionsDir, ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);

        restoreAllConnections(authorityGroupsDir, ManifoldAPI.COMMANDS.AUTHORITYGROUPS);

        restoreAllConnections(authorityConnectionsDir, ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);

        restoreAllConnections(mappingConnectionsDir, ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);

        restoreAllConnections(transformationConnectionsDir, ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);

        restoreAllConnections(repositoryConnectionsDir, ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);

        restoreAllConnections(jobsDir, ManifoldAPI.COMMANDS.JOBS);

        LOGGER.info("Connectors Restored");

      } catch (final Exception e) {
        LOGGER.error("Error while restoring MCF connections", e);
        throw new Exception("Error while restoring MCF connections");
      }
    }
  }

  private static boolean checkRestoreFiles(final File[] files) throws Exception {
    final boolean status = true;
    for (int i = 0; i < files.length; i++) {
      final File f = files[i];
      if (!f.canRead()) {
        throw new IOException("Cannot read : " + f.getAbsolutePath());
      } else if (!f.isDirectory() || !restoreDirectoriesNames.contains(f.getName())) {
        LOGGER.warn("The provided directory does not contain a correct file tree");
        throw new Exception("The provided directory does not contain a correct file tree");
      } else {
        final File[] subfiles = f.listFiles();
        for (int j = 0; j < subfiles.length; j++) {
          final File subFile = subfiles[j];
          final JSONParser parser = new JSONParser();
          try {
            parser.parse(new FileReader(subFile));
          } catch (final Exception e) {
            LOGGER.warn("The file " + subFile.getAbsolutePath() + " is not a JSON file");
            throw new Exception("The file " + subFile.getAbsolutePath() + " is not a JSON file");
          }
        }
      }
    }
    return status;
  }
}

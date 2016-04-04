package com.francelabs.manifoldcf.configuration.script;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;
import org.json.JSONObject;

import com.francelabs.manifoldcf.configuration.api.JSONUtils;
import com.francelabs.manifoldcf.configuration.api.ManifoldAPI;

public class BackupManifoldCFConnectorsScript {

	// TODO check if the logs are working properly
	private static String configPropertiesFileName = "config/log4j.properties";

	private final static Logger LOGGER = Logger.getLogger(BackupManifoldCFConnectorsScript.class);

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) {

		PropertyConfigurator.configure(configPropertiesFileName);

		if (args.length < 1) {
			LOGGER.fatal("No argument");
			return;
		}

		String backupDirectory;
		if (args.length < 2) {
			System.out.println("Backup directory : ");
			Scanner input = new Scanner(System.in);
			backupDirectory = input.nextLine();
		} else {
			backupDirectory = args[1];
		}

		try {

			File outputConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);

			File repositoryConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);

			File authorityConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);
			
			File mappingConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);
			
			File transformationConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);
			
			File authorityGroupsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYGROUPS);

			File jobsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.JOBS);

			if (args[0].equals("BACKUP")) {

				ManifoldAPI.waitUntilManifoldIsStarted();

				prepareDirectory(outputConnectionsDir);
				saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS),
						outputConnectionsDir);

				prepareDirectory(repositoryConnectionsDir);
				saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS),
						repositoryConnectionsDir);

				prepareDirectory(authorityConnectionsDir);
				saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS),
						authorityConnectionsDir);
				
				prepareDirectory(mappingConnectionsDir);
				saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS),
						mappingConnectionsDir);
				
				prepareDirectory(transformationConnectionsDir);
				saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS),
						transformationConnectionsDir);
				
				prepareDirectory(authorityGroupsDir);
				saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.AUTHORITYGROUPS),
						authorityGroupsDir);

				prepareDirectory(jobsDir);
				saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.JOBS), jobsDir);

				LOGGER.info("Connectors Saved");

			}

			if (args[0].equals("RESTORE")) {
				ManifoldAPI.waitUntilManifoldIsStarted();
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

		} catch (Exception e) {
			LOGGER.fatal(e.getMessage());
			e.printStackTrace();
		}
	}

	private static void prepareDirectory(File directory) {
		directory.mkdirs();
		for (File file : directory.listFiles()) {
			file.delete();
		}
	}

	private static void restoreAllConnections(File directory, String command) throws Exception {

		File[] connectorFiles = directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				name.endsWith(".json");
				return true;
			}
		});
		for (File connectorFile : connectorFiles) {
			restoreConnection(connectorFile, command);
		}

	}

	private static void restoreConnection(File connectorFile, String command) throws Exception {

		JSONObject jsonObject = JSONUtils.readJSON(connectorFile);
		String name = connectorFile.getName();
		ManifoldAPI.putConfig(command, name.substring(0, name.length() - 5), jsonObject);

	}

	private static void saveAllConnections(Map<String, JSONObject> connections, File directory)
			throws IOException, JSONException {

		for (Entry<String, JSONObject> connection : connections.entrySet()) {
			saveConnection(connection, directory);
		}
	}

	private static void saveConnection(Entry<String, JSONObject> outputConnection, File directory)
			throws IOException, JSONException {

		JSONUtils.saveJSON(outputConnection.getValue(), new File(directory, outputConnection.getKey() + ".json"));
		File connectorFile = new File(directory, outputConnection.getKey() + ".json");

	}

	/**
	 * Method called by Servlet MCFBackupRestore
	 */
	public static void doSave(String backupDirectory) throws Exception {

		File outputConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);
		
		File authorityGroupsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYGROUPS);
		
		File repositoryConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);

		File authorityConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);
		
		File mappingConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);
		
		File transformationConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);

		File jobsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.JOBS);

		try {
			
			ManifoldAPI.waitUntilManifoldIsStarted();

			prepareDirectory(outputConnectionsDir);
			saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS),
					outputConnectionsDir);

			prepareDirectory(repositoryConnectionsDir);
			saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS),
					repositoryConnectionsDir);
			
			prepareDirectory(authorityGroupsDir);
			saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.AUTHORITYGROUPS),
					authorityGroupsDir);

			prepareDirectory(authorityConnectionsDir);
			saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS),
					authorityConnectionsDir);
			
			prepareDirectory(mappingConnectionsDir);
			saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS),
					mappingConnectionsDir);
			
			prepareDirectory(transformationConnectionsDir);
			saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS),
					transformationConnectionsDir);

			prepareDirectory(jobsDir);
			saveAllConnections(ManifoldAPI.getConnections(ManifoldAPI.COMMANDS.JOBS), jobsDir);

			LOGGER.info("Connectors Saved");

		} catch (Exception e) {
			LOGGER.fatal(e.getMessage());
			throw new Exception("Error while saving MCF connections.");
		}
	}
	
	/**
	 * Method called by Servlet MCFBackupRestore
	 */
	public static void doRestore(String backupDirectory) throws Exception {

		File outputConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);
		
		File authorityGroupsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYGROUPS);
		
		File transformationConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);
		
		File mappingConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);
		
		File authorityConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);

		File repositoryConnectionsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);

		File jobsDir = new File(backupDirectory, ManifoldAPI.COMMANDS.JOBS);

		try {
			
			ManifoldAPI.waitUntilManifoldIsStarted();
			ManifoldAPI.cleanAll();

			restoreAllConnections(outputConnectionsDir, ManifoldAPI.COMMANDS.OUTPUTCONNECTIONS);
			
			restoreAllConnections(authorityGroupsDir, ManifoldAPI.COMMANDS.AUTHORITYGROUPS);
			
			restoreAllConnections(authorityConnectionsDir, ManifoldAPI.COMMANDS.AUTHORITYCONNECTIONS);
			
			restoreAllConnections(mappingConnectionsDir, ManifoldAPI.COMMANDS.MAPPINGCONNECTIONS);
			
			restoreAllConnections(transformationConnectionsDir, ManifoldAPI.COMMANDS.TRANSFORMATIONCONNECTIONS);

			restoreAllConnections(repositoryConnectionsDir, ManifoldAPI.COMMANDS.REPOSITORYCONNECTIONS);
			
			restoreAllConnections(jobsDir, ManifoldAPI.COMMANDS.JOBS);

			LOGGER.info("Connectors Restored");

		} catch (Exception e) {
			LOGGER.fatal(e.getMessage());
			throw new Exception("Error while restoring MCF connections.");
		}
	}
}

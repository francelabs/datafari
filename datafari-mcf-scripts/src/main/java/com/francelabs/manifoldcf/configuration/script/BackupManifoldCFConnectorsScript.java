package com.francelabs.manifoldcf.configuration.script;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.manifoldcf.core.interfaces.IThreadContext;
import org.apache.manifoldcf.core.interfaces.LockManagerFactory;
import org.apache.manifoldcf.core.interfaces.ThreadContextFactory;
import org.apache.manifoldcf.core.system.ManifoldCF;
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
	public static void main(final String[] args) {

		PropertyConfigurator.configure(configPropertiesFileName);

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

			final IThreadContext tc = ThreadContextFactory.make();
			ManifoldCF.initializeEnvironment(tc);

			final String masterDatabaseUsername = LockManagerFactory.getStringProperty(tc, "org.apache.manifoldcf.apilogin.password.obfuscated", "");

			ManifoldAPI.waitUntilManifoldIsStarted();

			if (!"".equals(masterDatabaseUsername)) {
				final String apiPassword = ManifoldCF.deobfuscate(masterDatabaseUsername);
				LOGGER.info("Try to authenticate");
				ManifoldAPI.authenticate("", apiPassword);
			}

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

		} catch (final Exception e) {
			LOGGER.fatal(e.getMessage());
			e.printStackTrace();
		}
	}

	private static void prepareDirectory(final File directory) {
		directory.mkdirs();
		for (final File file : directory.listFiles()) {
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
		final String name = connectorFile.getName();
		ManifoldAPI.putConfig(command, name.substring(0, name.length() - 5), jsonObject);

	}

	private static void saveAllConnections(final Map<String, JSONObject> connections, final File directory) throws IOException, JSONException {

		for (final Entry<String, JSONObject> connection : connections.entrySet()) {
			saveConnection(connection, directory);
		}
	}

	private static void saveConnection(final Entry<String, JSONObject> outputConnection, final File directory) throws IOException, JSONException {

		JSONUtils.saveJSON(outputConnection.getValue(), new File(directory, outputConnection.getKey() + ".json"));
		final File connectorFile = new File(directory, outputConnection.getKey() + ".json");

	}

	/**
	 * Method called by Servlet MCFBackupRestore
	 */
	public static void doSave(final String backupDirectory) throws Exception {

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
			LOGGER.fatal(e.getMessage());
			throw new Exception("Error while saving MCF connections.");
		}
	}

	/**
	 * Method called by Servlet MCFBackupRestore
	 */
	public static void doRestore(final String backupDirectory) throws Exception {

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
			LOGGER.fatal(e.getMessage());
			throw new Exception("Error while restoring MCF connections.");
		}
	}
}

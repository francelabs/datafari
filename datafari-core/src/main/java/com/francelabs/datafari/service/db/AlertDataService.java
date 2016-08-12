package com.francelabs.datafari.service.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.DriverException;
import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.exception.DatafariServerException;

public class AlertDataService {

	public static final String ALERTCOLLECTION = "alerts";

	private Session session;

	private static AlertDataService instance;

	final static Logger logger = Logger.getLogger(AlertDataService.class.getName());

	public static synchronized AlertDataService getInstance() throws IOException {
		if (instance == null) {
			instance = new AlertDataService();
		}
		return instance;
	}

	public AlertDataService() throws IOException {

		// Gets the name of the collection
		session = DBContextListerner.getSession();
	}

	public void deleteAlert(String id) throws DatafariServerException {
		try {
			String query = "DELETE FROM " + ALERTCOLLECTION + " WHERE id =" + id + ";";
			session.execute(query);
		} catch (DriverException e) {
			logger.warn("Unable to delete alert : " + e.getMessage());
			// TODO catch specific exception
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}

	}

	public void addAlert(Properties alertProp) throws DatafariServerException {
		try {

			String query = "insert into " + ALERTCOLLECTION
					+ " (id, keyword, core, frequency, mail, subject, user) values (" + "uuid()," + "'"
					+ alertProp.getProperty("keyword") + "'," + "'" + alertProp.getProperty("core") + "'," + "'"
					+ alertProp.getProperty("frequency") + "'," + "'" + alertProp.getProperty("mail") + "'," + "'"
					+ alertProp.getProperty("subject") + "'," + "'" + alertProp.getProperty("user") + "');";
			session.execute(query);
		} catch (DriverException e) {
			logger.warn("Unable addAlert : " + e.getMessage());
			// TODO catch specific exception
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}
	}

	public List<Properties> getAlerts() throws DatafariServerException {
		try {
			List<Properties> alerts = new ArrayList<Properties>();
			ResultSet results = session.execute("SELECT * FROM " + ALERTCOLLECTION);
			for (Row row : results) {
				Properties alertProp = new Properties();
				UUID id = row.getUUID("id");
				alertProp.put("_id", id.toString());
				alertProp.put("keyword", row.getString("keyword"));
				alertProp.put("core", row.getString("core"));
				alertProp.put("frequency", row.getString("frequency"));
				alertProp.put("mail", row.getString("mail"));
				alertProp.put("subject", row.getString("subject"));
				alertProp.put("user", row.getString("user"));
				alerts.add(alertProp);
			}
			return alerts;
		} catch (DriverException e) {
			logger.warn("Unable getAlerts : " + e.getMessage());
			// TODO catch specific exception
			throw new DatafariServerException(CodesReturned.PROBLEMCONNECTIONDATABASE, e.getMessage());
		}
	}

}

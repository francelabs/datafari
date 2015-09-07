package com.francelabs.datafari.service.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class AlertDataService {

	public static final String ALERTCOLLECTION = "alerts";

	private Session session;


	private static AlertDataService instance;
	
	public static synchronized AlertDataService getInstance() throws IOException
	{
		if (instance == null){
			instance = new AlertDataService();
		}
		return instance;
	}
	
	public AlertDataService() throws IOException{
		
		// Gets the name of the collection
		session = DBContextListerner.getSession();
	}

	
	public void deleteAlert(String id){	
		String query = "DELETE FROM "+ALERTCOLLECTION+" WHERE id ="+ id +";";
		session.execute(query);

	}

	public void addAlert(Properties alertProp) {
		/*
		 * 
			 * id varchar PRIMARY KEY,
			 * keyword varchar,
			 * core varchar,
			 * frequency varchar,
			 * mail varchar,
			 * subject varchar,
			 * user varchar
		 * 
		 */
		String query = 
		"insert into "+ALERTCOLLECTION+" (id, keyword, core, frequency, mail, subject, user) values ("
		+   "uuid(),"
		+ 	"'" + alertProp.getProperty("keyword") + "',"
		+ 	"'" + alertProp.getProperty("core") + "',"
		+ 	"'" + alertProp.getProperty("frequency") + "',"
		+ 	"'" + alertProp.getProperty("mail") + "',"
		+ 	"'" + alertProp.getProperty("subject") + "',"
		+ 	"'" + alertProp.getProperty("user") + "');";
		session.execute(query);
	}

	public List<Properties> getAlerts() {
		List<Properties> alerts = new ArrayList<Properties>();
		ResultSet results = session.execute("SELECT * FROM "+ALERTCOLLECTION);
		for (Row row : results) {
			/*
			 * 
  			 * id varchar PRIMARY KEY,
  			 * keyword varchar,
  			 * core varchar,
  			 * frequency varchar,
  			 * mail varchar,
  			 * subject varchar,
  			 * user varchar
			 * 
			 */
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
	}

}

package com.francelabs.manifoldcf.configuration.api;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils {



	public static void saveJSON(JSONObject json, File file)
			throws java.io.IOException {
		file.createNewFile();	
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(json.toString());
		bw.close();
	}

	public static JSONObject readJSON(File file)
			throws java.io.IOException, JSONException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line, results = "";
		while ((line = reader.readLine()) != null) {
			results += line;
		}
		reader.close();
		return new JSONObject(results);
	}

}

package com.francelabs.manifoldcf.configuration.api;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSONUtils {

  public static void saveJSON(final JSONObject json, final File file) throws java.io.IOException {
    file.createNewFile();
    final FileWriter fw = new FileWriter(file.getAbsoluteFile());
    final BufferedWriter bw = new BufferedWriter(fw);
    bw.write(json.toJSONString());
    bw.close();
  }

  public static JSONObject readJSON(final File file) throws java.io.IOException, ParseException {
    final BufferedReader reader = new BufferedReader(new FileReader(file));
    String line, results = "";
    while ((line = reader.readLine()) != null) {
      results += line;
    }
    reader.close();
    final JSONParser parser = new JSONParser();
    return (JSONObject) parser.parse(results);
  }

  public static JSONObject cloneJSON(final JSONObject jsonObj) throws java.io.IOException, ParseException {
    final JSONParser parser = new JSONParser();
    return (JSONObject) parser.parse(jsonObj.toJSONString());
  }

}

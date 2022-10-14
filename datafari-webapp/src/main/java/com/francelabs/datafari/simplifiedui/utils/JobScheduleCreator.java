package com.francelabs.datafari.simplifiedui.utils;

import java.io.File;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.francelabs.datafari.utils.Environment;
import com.francelabs.manifoldcf.configuration.api.JSONUtils;

public class JobScheduleCreator {

  private static JobScheduleCreator instance = null;

  private final File scheduleJobConfJSON;

  private final static String value = "_value_";
  private final static String type = "_type_";
  private final static String childrenElement = "_children_";
  private final static String timezoneElement = "timezone";

  private JobScheduleCreator() {
    String datafariHomePath = Environment.getEnvironmentVariable("DATAFARI_HOME");
    if (datafariHomePath == null) {
      // if no variable is set, use the default installation path
      datafariHomePath = "/opt/datafari";
    }

    final String docFilterStagePath = datafariHomePath + File.separator + "bin" + File.separator + "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator
        + "simplifiedui" + File.separator + "utils" + File.separator + "scheduleJobConf.json";
    scheduleJobConfJSON = new File(docFilterStagePath);
  }

  public static JobScheduleCreator getInstance() {
    if (instance == null) {
      instance = new JobScheduleCreator();
    }
    return instance;
  }

  public JSONObject createDefaultJobSchedule(final String timezone) throws IOException, ParseException {
    final JSONObject scheduleJobConf = JSONUtils.readJSON(scheduleJobConfJSON);
    final JSONArray scheduleJobConfChildrenEl = (JSONArray) scheduleJobConf.get(childrenElement);
    for (int i = 0; i < scheduleJobConfChildrenEl.size(); i++) {
      final JSONObject child = (JSONObject) scheduleJobConfChildrenEl.get(i);
      if (child.get(type).equals(timezoneElement)) {
        child.replace(value, timezone);
      }
    }
    return scheduleJobConf;
  }

}

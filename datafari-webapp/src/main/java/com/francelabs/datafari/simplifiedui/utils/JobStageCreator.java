package com.francelabs.datafari.simplifiedui.utils;

import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class JobStageCreator {

  private static JobStageCreator instance = null;

  private JobStageCreator() {

  }

  public static JobStageCreator getInstance() {
    if (instance == null) {
      instance = new JobStageCreator();
    }
    return instance;
  }

  public JSONObject createDocFilterStage(final int stageId, final int prerequisiteStageId) throws IOException, ParseException {
    return null;
  }

  public JSONObject createDuplicatesOutputStage(final int stageId, final int prerequisiteStageId) throws IOException, ParseException {
    return null;
  }

}

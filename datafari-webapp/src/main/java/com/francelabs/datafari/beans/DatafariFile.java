package com.francelabs.datafari.beans;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DatafariFile {

  private String title = "";
  private Date last_modified;
  private String url = "";

  public DatafariFile(final JSONObject jsonDoc) {
    if (jsonDoc.get("title") != null) {
      title = (String) ((JSONArray) jsonDoc.get("title")).get(0);
    }
    if (jsonDoc.get("last_modified") != null) {
      String strDate = (String) ((JSONArray) jsonDoc.get("last_modified")).get(0);
      strDate = strDate.substring(0, strDate.indexOf("T"));
      final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
      try {
        last_modified = sdf.parse(strDate);
      } catch (final ParseException e) {
        last_modified = new Date();
      }
    }
    if (jsonDoc.get("url") != null) {
      url = (String) jsonDoc.get("url");
    }
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public Date getLast_modified() {
    return last_modified;
  }

  public void setLast_modified(final Date last_modified) {
    this.last_modified = last_modified;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

}

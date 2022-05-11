/* $Id: LinkParseState.java 1895187 2021-11-19 22:16:11Z kwright $ */

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.francelabs.datafari.connectors.webcrawler;

import org.apache.manifoldcf.core.interfaces.*;
import org.apache.manifoldcf.connectorcommon.fuzzyml.*;
import java.util.*;

/** This class recognizes and interprets all links */
public class LinkParseState extends MetaParseState
{

  protected IHTMLHandler handler;

  public LinkParseState(IHTMLHandler handler)
  {
    super(handler);
    this.handler = handler;
  }

  @Override
  protected boolean noteNonscriptTag(String tagName, Map<String,String> attributes)
    throws ManifoldCFException
  {
    if (super.noteNonscriptTag(tagName,attributes))
      return true;
    if (tagName.equals("a"))
    {
      String hrefValue = (String)attributes.get("href");
      if (hrefValue != null && hrefValue.length() > 0)
        handler.noteAHREF(hrefValue);
    }
    else if (tagName.equals("link"))
    {
      String hrefValue = (String)attributes.get("href");
      if (hrefValue != null && hrefValue.length() > 0)
        handler.noteLINKHREF(hrefValue);
    }
    else if (tagName.equals("base"))
    {
      String hrefValue = (String)attributes.get("href");
      if (hrefValue != null && hrefValue.length() > 0)
        handler.noteBASEHREF(hrefValue);
    }
    else if (tagName.equals("img"))
    {
      String srcValue = (String)attributes.get("src");
      if (srcValue != null && srcValue.length() > 0)
        handler.noteIMGSRC(srcValue);
    }
    else if (tagName.equals("frame"))
    {
      String srcValue = (String)attributes.get("src");
      if (srcValue != null && srcValue.length() > 0)
        handler.noteFRAMESRC(srcValue);
    }
    return false;
  }

  @Override
  public void finishUp()
    throws ManifoldCFException
  {
    handler.finishUp();
    super.finishUp();
  }
  
}

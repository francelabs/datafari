/* $Id: FindHTMLFormHandler.java 1895187 2021-11-19 22:16:11Z kwright $ */

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
import org.apache.manifoldcf.crawler.system.Logging;
import java.util.regex.*;
import java.util.*;

/** This class is the handler for HTML form parsing during state transitions */
public class FindHTMLFormHandler extends FindHandler implements IHTMLHandler
{
  protected final Pattern formNamePattern;
  protected FormDataAccumulator discoveredFormData = null;
  protected FormDataAccumulator currentFormData = null;

  public FindHTMLFormHandler(String parentURI, Pattern formNamePattern)
  {
    super(parentURI);
    this.formNamePattern = formNamePattern;
  }

  public void applyFormOverrides(LoginParameters lp)
    throws ManifoldCFException
  {
    if (discoveredFormData != null && lp != null)
    {
      if (lp.getOverrideTargetURL() != null)
      {
        super.noteDiscoveredLink(lp.getOverrideTargetURL());
        discoveredFormData.overrideActionURI(getTargetURI());
      }
      discoveredFormData.applyOverrides(lp);
    }
  }

  public FormData getFormData()
  {
    return discoveredFormData;
  }

  /** Note a character of text.
  * Structured this way to keep overhead low for handlers that don't use text.
  */
  @Override
  public void noteTextCharacter(char textCharacter)
    throws ManifoldCFException
  {
  }

  /** Note a meta tag */
  @Override
  public void noteMetaTag(Map metaAttributes)
    throws ManifoldCFException
  {
  }
    
  /** Note the start of a form */
  @Override
  public void noteFormStart(Map formAttributes)
    throws ManifoldCFException
  {
    if (Logging.connectors.isDebugEnabled())
      Logging.connectors.debug("WEB: Saw form with"+
        " name "+((formAttributes.get("name")==null)?"null":"'"+formAttributes.get("name")+"'") +
        " id "+((formAttributes.get("id")==null)?"null":"'"+formAttributes.get("id")+"'") +
        " action "+((formAttributes.get("action")==null)?"null":"'"+formAttributes.get("action")+"'")
      );

    // Is this a form element we can use?
    boolean canUse;
    if (formNamePattern != null)
    {
      // Find the identifier we will use for the form.  If name isn't there,
      // we use id.  If id isn't there, we use action.  The only other thing we
      // could reasonably do is identify the form by its form elements.
      String formName = (String)formAttributes.get("name");
      if (formName == null)
        formName = (String)formAttributes.get("id");
      if (formName == null)
        formName = (String)formAttributes.get("action");
      if (formName == null)
        formName = "";

      Matcher m = formNamePattern.matcher(formName);
      canUse = m.find();
    }
    else
      canUse = true;

    if (canUse)
    {
      String actionURI = (String)formAttributes.get("action");
      if (actionURI == null)
        // Action URI is THIS uri!
        actionURI = parentURI;
      else if (actionURI.length() == 0)
        actionURI = "";
      noteDiscoveredLink(actionURI);
      actionURI = getTargetURI();
      if (actionURI != null)
      {
        String method = (String)formAttributes.get("method");
        if (method == null || method.length() == 0)
          method = "get";
        else
          method = method.toLowerCase(Locale.ROOT);

        // Start a new form
        currentFormData = new FormDataAccumulator(actionURI,method.equals("post")?FormData.SUBMITMETHOD_POST:FormData.SUBMITMETHOD_GET);

      }
    }
  }

  /** Note an input tag */
  @Override
  public void noteFormInput(Map inputAttributes)
    throws ManifoldCFException
  {
    if (Logging.connectors.isDebugEnabled())
    {
      String type = (String)inputAttributes.get("type");
      if (type == null)
        type = "text";
      String name = (String)inputAttributes.get("name");
      if (name == null)
        name = "(null)";
      Logging.connectors.debug("WEB: Saw form element of type '"+type+"' name '"+name+"'");
    }
    if (currentFormData != null)
      currentFormData.addElement(inputAttributes);
  }

  /** Note the end of a form */
  @Override
  public void noteFormEnd()
    throws ManifoldCFException
  {
    if (currentFormData != null)
    {
      discoveredFormData = currentFormData;
      currentFormData = null;
    }
  }

  /** Note discovered base href */
  @Override
  public void noteBASEHREF(String rawURL)
    throws ManifoldCFException
  {
  }
  
  /** Note discovered href */
  @Override
  public void noteAHREF(String rawURL)
    throws ManifoldCFException
  {
  }

  /** Note discovered href */
  @Override
  public void noteLINKHREF(String rawURL)
    throws ManifoldCFException
  {
  }

  /** Note discovered IMG SRC */
  @Override
  public void noteIMGSRC(String rawURL)
    throws ManifoldCFException
  {
  }

  /** Note discovered FRAME SRC */
  @Override
  public void noteFRAMESRC(String rawURL)
    throws ManifoldCFException
  {
  }

  @Override
  public void finishUp()
    throws ManifoldCFException
  {
  }

}

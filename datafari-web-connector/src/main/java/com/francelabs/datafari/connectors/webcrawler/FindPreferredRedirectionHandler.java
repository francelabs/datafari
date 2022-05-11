/* $Id: FindPreferredRedirectionHandler.java 1895187 2021-11-19 22:16:11Z kwright $ */

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

/** This class is the handler for redirection handling during state transitions */
public class FindPreferredRedirectionHandler extends FindHandler implements IRedirectionHandler
{
  protected Pattern redirectionURIPattern;

  public FindPreferredRedirectionHandler(String parentURI, Pattern redirectionURIPattern)
  {
    super(parentURI);
    this.redirectionURIPattern = redirectionURIPattern;
  }

  /** Apply overrides */
  public void applyOverrides(LoginParameters lp)
    throws ManifoldCFException
  {
    if (targetURI != null && lp != null)
    {
      if (lp.getOverrideTargetURL() != null)
        super.noteDiscoveredLink(lp.getOverrideTargetURL());
    }
  }

  @Override
  public void noteDiscoveredBase(String rawURL)
    throws ManifoldCFException
  {
    super.noteDiscoveredBase(rawURL);
  }
  
  /** Override noteDiscoveredLink */
  @Override
  public void noteDiscoveredLink(String rawURL)
    throws ManifoldCFException
  {
    if (targetURI == null)
    {
      Logging.connectors.debug("WEB: Tried to match raw url '"+rawURL+"'");
      super.noteDiscoveredLink(rawURL);
      if (targetURI != null)
      {
        Logging.connectors.debug("WEB: Tried to match cooked url '"+targetURI+"'");
        // Is this a form element we can use?
        boolean canUse;
        if (redirectionURIPattern != null)
        {
          Matcher m = redirectionURIPattern.matcher(targetURI);
          canUse = m.find();
          Logging.connectors.debug("WEB: Redirection link lookup "+((canUse)?"matched":"didn't match")+" '"+targetURI+"'");
        }
        else
        {
          Logging.connectors.debug("WEB: Redirection link lookup for '"+targetURI+"' had no pattern to match");
          canUse = true;
        }
        if (!canUse)
          targetURI = null;
      }
    }
  }
}


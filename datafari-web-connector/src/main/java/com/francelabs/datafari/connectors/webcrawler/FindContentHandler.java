/* $Id: FindContentHandler.java 1895187 2021-11-19 22:16:11Z kwright $ */

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

import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import static java.util.Arrays.asList;
import static org.apache.manifoldcf.crawler.system.Logging.connectors;

/** This class is the handler for HTML content grepping during state transitions */
public class FindContentHandler extends FindHandler implements IHTMLHandler
{
  protected final List<Pattern> contentPatterns;
  protected final StringBuilder contentBuffer = new StringBuilder();

  protected final static int MAX_LENGTH = 65536;
  protected final static int OVERLAP_AMOUNT = 16384;
  
  public FindContentHandler(String parentURI, Pattern contentPattern)
  {
    super(parentURI);
    this.contentPatterns = asList(contentPattern);
  }

  public FindContentHandler(String parentURI, List<Pattern> contentPatterns)
  {
    super(parentURI);
    this.contentPatterns = contentPatterns;
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

  /** Note a character of text.
  * Structured this way to keep overhead low for handlers that don't use text.
  */
  @Override
  public void noteTextCharacter(char textCharacter)
    throws ManifoldCFException
  {
    if (targetURI != null)
      return;
    // Build characters up into lines, and apply the regexp against them
    if (textCharacter == '\t' || textCharacter >= ' ')
    {
      contentBuffer.append(textCharacter);
      // If too big, do the search and clear out the buffer, retaining some of it for overlap purposes
      if (contentBuffer.length() >= MAX_LENGTH)
      {
        // Process what we have, and keep around what we need for
        // continuity
        String bufferContents = contentBuffer.toString();
        contentBuffer.setLength(0);
        for (Pattern contentPattern : contentPatterns) {
          if (contentPattern.matcher(bufferContents).find()) {
            targetURI = "";
            break;
          }
        }

        if(targetURI == null) {
          contentBuffer.append(bufferContents.substring(bufferContents.length() - OVERLAP_AMOUNT));
        }
      }
    }
    else
    {
      processBuffer();
    }
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
  }

  /** Note an input tag */
  @Override
  public void noteFormInput(Map inputAttributes)
    throws ManifoldCFException
  {
  }

  /** Note the end of a form */
  @Override
  public void noteFormEnd()
    throws ManifoldCFException
  {
  }

  /** Note discovered href */
  @Override
  public void noteAHREF(String rawURL)
    throws ManifoldCFException
  {
  }

  /** Note discovered base href */
  @Override
  public void noteBASEHREF(String rawURL)
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

  /** Finish up all processing.  Called ONLY if we haven't already aborted.
  */
  @Override
  public void finishUp()
    throws ManifoldCFException
  {
    if (targetURI == null)
      processBuffer();
  }

  protected void processBuffer()
  {
    String bufferContents = contentBuffer.toString();
    contentBuffer.setLength(0);
    for(Pattern contentPattern: contentPatterns) {
      if (contentPattern.matcher(bufferContents).find()) {
        targetURI = "";
        return;
      }
    }
  }

}

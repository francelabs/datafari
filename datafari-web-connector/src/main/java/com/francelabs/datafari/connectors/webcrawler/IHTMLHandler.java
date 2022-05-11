/* $Id: IHTMLHandler.java 1895187 2021-11-19 22:16:11Z kwright $ */

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
import java.util.*;

/** This interface describes the functionality needed by an HTML processor in order to handle an HTML document.
*/
public interface IHTMLHandler extends IDiscoveredLinkHandler, IMetaTagHandler
{
  /** Note the start of a form */
  public void noteFormStart(Map formAttributes)
    throws ManifoldCFException;

  /** Note an input tag */
  public void noteFormInput(Map inputAttributes)
    throws ManifoldCFException;

  /** Note the end of a form */
  public void noteFormEnd()
    throws ManifoldCFException;

  /** Note discovered href */
  public void noteAHREF(String rawURL)
    throws ManifoldCFException;

  /** Note discovered href */
  public void noteLINKHREF(String rawURL)
    throws ManifoldCFException;

  /** Note base href */
  public void noteBASEHREF(String rawURL)
    throws ManifoldCFException;
    
  /** Note discovered IMG SRC */
  public void noteIMGSRC(String rawURL)
    throws ManifoldCFException;

  /** Note discovered FRAME SRC */
  public void noteFRAMESRC(String rawURL)
    throws ManifoldCFException;

  /** Note a character of text.
  * Structured this way to keep overhead low for handlers that don't use text.
  */
  public void noteTextCharacter(char textCharacter)
    throws ManifoldCFException;

  /** Done with the document.
  */
  public void finishUp()
    throws ManifoldCFException;

}

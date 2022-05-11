/* $Id: LoginParameters.java 988245 2010-08-23 18:39:35Z kwright $ */

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

import java.util.regex.*;

/** This interface describes login parameters to be used to submit a page during sequential authentication.
*/
public interface LoginParameters
{
  public static final String _rcsid = "@(#)$Id: LoginParameters.java 988245 2010-08-23 18:39:35Z kwright $";

  /** Get the override target URL.
  */
  public String getOverrideTargetURL();

  /** Get the preferred redirection pattern.
  */
  public Pattern getPreferredRedirectionPattern();

  /** Get the preferred link pattern.
  */
  public Pattern getPreferredLinkPattern();

  /** Get the form name pattern.
  */
  public Pattern getFormNamePattern();

  /** Get the content pattern.
  */
  public Pattern getContentPattern();
  
  /** Get the number of parameters.
  */
  public int getParameterCount();

  /** Get the name of the i'th parameter.
  */
  public Pattern getParameterNamePattern(int index);

  /** Get the desired value of the i'th parameter.
  */
  public String getParameterValue(int index);

}

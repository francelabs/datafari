/* $Id$ */

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
package org.apache.manifoldcf.authorities.authorities.jira;

import java.util.Locale;
import java.util.Map;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
import org.apache.manifoldcf.core.interfaces.IHTTPOutput;

public class Messages extends org.apache.manifoldcf.ui.i18n.Messages
{
  public static final String DEFAULT_BUNDLE_NAME="org.apache.manifoldcf.authorities.authorities.jira.common";
  public static final String DEFAULT_PATH_NAME="org.apache.manifoldcf.authorities.authorities.jira";
  
  /** Constructor - do no instantiate
  */
  protected Messages()
  {
  }
  
  public static String getString(Locale locale, String messageKey)
  {
    return getString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getAttributeString(Locale locale, String messageKey)
  {
    return getAttributeString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getBodyString(Locale locale, String messageKey)
  {
    return getBodyString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getAttributeJavascriptString(Locale locale, String messageKey)
  {
    return getAttributeJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getBodyJavascriptString(Locale locale, String messageKey)
  {
    return getBodyJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getString(Locale locale, String messageKey, Object[] args)
  {
    return getString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  public static String getAttributeString(Locale locale, String messageKey, Object[] args)
  {
    return getAttributeString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }
  
  public static String getBodyString(Locale locale, String messageKey, Object[] args)
  {
    return getBodyString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  public static String getAttributeJavascriptString(Locale locale, String messageKey, Object[] args)
  {
    return getAttributeJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  public static String getBodyJavascriptString(Locale locale, String messageKey, Object[] args)
  {
    return getBodyJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  // More general methods which allow bundlenames and class loaders to be specified.
  
  public static String getString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getAttributeString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getAttributeString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getBodyString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getBodyString(Messages.class, bundleName, locale, messageKey, args);
  }
  
  public static String getAttributeJavascriptString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getAttributeJavascriptString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getBodyJavascriptString(String bundleName, Locale locale, String messageKey, Object[] args)
  {
    return getBodyJavascriptString(Messages.class, bundleName, locale, messageKey, args);
  }

  // Resource output
  
  public static void outputResource(IHTTPOutput output, Locale locale, String resourceKey,
    Map<String,String> substitutionParameters, boolean mapToUpperCase)
    throws ManifoldCFException
  {
    outputResource(output,Messages.class,DEFAULT_PATH_NAME,locale,resourceKey,
      substitutionParameters,mapToUpperCase);
  }
  
  public static void outputResourceWithVelocity(IHTTPOutput output, Locale locale, String resourceKey,
    Map<String,String> substitutionParameters, boolean mapToUpperCase)
    throws ManifoldCFException
  {
    outputResourceWithVelocity(output,Messages.class,DEFAULT_BUNDLE_NAME,DEFAULT_PATH_NAME,locale,resourceKey,
      substitutionParameters,mapToUpperCase);
  }

  public static void outputResourceWithVelocity(IHTTPOutput output, Locale locale, String resourceKey,
    Map<String,Object> contextObjects)
    throws ManifoldCFException
  {
    outputResourceWithVelocity(output,Messages.class,DEFAULT_BUNDLE_NAME,DEFAULT_PATH_NAME,locale,resourceKey,
      contextObjects);
  }
  
}


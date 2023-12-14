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

package org.apache.manifoldcf.authorities.authorities.confluence.v6;

import java.util.Locale;
import java.util.Map;

import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

/**
 * <p>
 * Messages class
 * </p>
 * <p>
 * Class used to render templates along with specific values
 * </p>
 * <p>
 * Also used to get messages for specific Locales
 * </p>
 *
 * @author Antonio David Perez Morales &lt;adperezmorales@gmail.com&gt;
 *
 */
public class Messages extends org.apache.manifoldcf.ui.i18n.Messages {
  public static final String DEFAULT_BUNDLE_NAME = "org.apache.manifoldcf.authorities.authorities.confluence.v6.common";
  public static final String DEFAULT_PATH_NAME = "org.apache.manifoldcf.authorities.authorities.confluence.v6";

  /**
   * Constructor - do no instantiate
   */
  protected Messages() {
  }

  public static String getString(final Locale locale, final String messageKey) {
    return getString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getAttributeString(final Locale locale, final String messageKey) {
    return getAttributeString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getBodyString(final Locale locale, final String messageKey) {
    return getBodyString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getAttributeJavascriptString(final Locale locale, final String messageKey) {
    return getAttributeJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getBodyJavascriptString(final Locale locale, final String messageKey) {
    return getBodyJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, null);
  }

  public static String getString(final Locale locale, final String messageKey, final Object[] args) {
    return getString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  public static String getAttributeString(final Locale locale, final String messageKey, final Object[] args) {
    return getAttributeString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  public static String getBodyString(final Locale locale, final String messageKey, final Object[] args) {
    return getBodyString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  public static String getAttributeJavascriptString(final Locale locale, final String messageKey, final Object[] args) {
    return getAttributeJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  public static String getBodyJavascriptString(final Locale locale, final String messageKey, final Object[] args) {
    return getBodyJavascriptString(DEFAULT_BUNDLE_NAME, locale, messageKey, args);
  }

  // More general methods which allow bundlenames and class loaders to be specified.

  public static String getString(final String bundleName, final Locale locale, final String messageKey, final Object[] args) {
    return getString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getAttributeString(final String bundleName, final Locale locale, final String messageKey, final Object[] args) {
    return getAttributeString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getBodyString(final String bundleName, final Locale locale, final String messageKey, final Object[] args) {
    return getBodyString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getAttributeJavascriptString(final String bundleName, final Locale locale, final String messageKey, final Object[] args) {
    return getAttributeJavascriptString(Messages.class, bundleName, locale, messageKey, args);
  }

  public static String getBodyJavascriptString(final String bundleName, final Locale locale, final String messageKey, final Object[] args) {
    return getBodyJavascriptString(Messages.class, bundleName, locale, messageKey, args);
  }

  // Resource output

  public static void outputResource(final IHTTPOutput output, final Locale locale, final String resourceKey, final Map<String, String> substitutionParameters, final boolean mapToUpperCase)
      throws ManifoldCFException {
    outputResource(output, Messages.class, DEFAULT_PATH_NAME, locale, resourceKey, substitutionParameters, mapToUpperCase);
  }

  public static void outputResourceWithVelocity(final IHTTPOutput output, final Locale locale, final String resourceKey, final Map<String, String> substitutionParameters, final boolean mapToUpperCase)
      throws ManifoldCFException {
    outputResourceWithVelocity(output, Messages.class, DEFAULT_BUNDLE_NAME, DEFAULT_PATH_NAME, locale, resourceKey, substitutionParameters, mapToUpperCase);
  }

  public static void outputResourceWithVelocity(final IHTTPOutput output, final Locale locale, final String resourceKey, final Map<String, Object> contextObjects) throws ManifoldCFException {
    outputResourceWithVelocity(output, Messages.class, DEFAULT_BUNDLE_NAME, DEFAULT_PATH_NAME, locale, resourceKey, contextObjects);
  }

}

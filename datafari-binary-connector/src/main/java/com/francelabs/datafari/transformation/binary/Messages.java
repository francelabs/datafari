package com.francelabs.datafari.transformation.binary;

import java.util.Locale;
import java.util.Map;

import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
import org.apache.manifoldcf.core.interfaces.ManifoldCFException;

public class Messages extends org.apache.manifoldcf.ui.i18n.Messages {
  public static final String DEFAULT_BUNDLE_NAME = "com.francelabs.datafari.transformation.binary.common";
  public static final String DEFAULT_PATH_NAME = "com.francelabs.datafari.transformation.binary";

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

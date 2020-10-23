package com.francelabs.datafari.annotator.exception;

public class RegexException extends Exception {

  private String regex = "";

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public RegexException(final String regex, final String message) {
    super(message);
    this.regex = regex;
  }

  public RegexException(final String regex, final String message, final Exception e) {
    super(message, e);
    this.regex = regex;
  }

  public String getRegex() {
    return regex;
  }

}

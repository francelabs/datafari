package com.francelabs.datafari.annotator.exception;

public class AnnotatorException extends Exception {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public AnnotatorException(final String message) {
    super(message);
  }

  public AnnotatorException(final Exception e) {
    super(e);
  }

  public AnnotatorException(final String message, final Exception e) {
    super(message, e);
  }

}

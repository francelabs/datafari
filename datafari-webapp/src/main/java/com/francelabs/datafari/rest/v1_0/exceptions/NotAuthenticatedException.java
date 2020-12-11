package com.francelabs.datafari.rest.v1_0.exceptions;

public class NotAuthenticatedException extends RuntimeException {

    private static final long serialVersionUID = -7963178563577712321L;

    public NotAuthenticatedException() {
        super();
    }

    public NotAuthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotAuthenticatedException(String message) {
        super(message);
    }

    public NotAuthenticatedException(Throwable cause) {
        super(cause);
    }
}

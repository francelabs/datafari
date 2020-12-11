package com.francelabs.datafari.rest.v1_0.exceptions;

public class InternalErrorException extends RuntimeException {

    private static final long serialVersionUID = -7963178563577712323L;

    public InternalErrorException() {
        super();
    }

    public InternalErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalErrorException(String message) {
        super(message);
    }

    public InternalErrorException(Throwable cause) {
        super(cause);
    }
    
}

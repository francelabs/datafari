package com.francelabs.datafari.rest.v1_0.exceptions;

public class DataNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = -7963178563598712322L;

    public DataNotFoundException() {
        super();
    }

    public DataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataNotFoundException(String message) {
        super(message);
    }

    public DataNotFoundException(Throwable cause) {
        super(cause);
    }
}

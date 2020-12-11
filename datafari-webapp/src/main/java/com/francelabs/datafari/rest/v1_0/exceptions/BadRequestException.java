package com.francelabs.datafari.rest.v1_0.exceptions;

public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = -7963178563577712322L;

    public BadRequestException() {
        super();
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

}

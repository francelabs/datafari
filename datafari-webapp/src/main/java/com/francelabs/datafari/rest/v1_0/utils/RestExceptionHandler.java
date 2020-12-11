package com.francelabs.datafari.rest.v1_0.utils;

import com.francelabs.datafari.rest.v1_0.exceptions.BadRequestException;
import com.francelabs.datafari.rest.v1_0.exceptions.DataNotFoundException;
import com.francelabs.datafari.rest.v1_0.exceptions.InternalErrorException;
import com.francelabs.datafari.rest.v1_0.exceptions.NotAuthenticatedException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = { NotAuthenticatedException.class })
    protected ResponseEntity<Object> handleNotAuthenticated(RuntimeException ex, WebRequest request) {
        String body = RestAPIUtils.buildErrorResponse(HttpStatus.UNAUTHORIZED.value(),
                "User is not authenticated or username could not be determined and this feature requires to be logged in.",
                null);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        return handleExceptionInternal(ex, body, headers, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value = { BadRequestException.class })
    protected ResponseEntity<Object> handleBadRequest(RuntimeException ex, WebRequest request) {
        String body = RestAPIUtils.buildErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getLocalizedMessage(), null);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { InternalErrorException.class })
    protected ResponseEntity<Object> handleInternalError(RuntimeException ex, WebRequest request) {
        String body = RestAPIUtils.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getLocalizedMessage(), null);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        return handleExceptionInternal(ex, body, headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(value = {DataNotFoundException.class})
    protected ResponseEntity<Object> handleDataNotFound(RuntimeException ex, WebRequest request) {
        String body = RestAPIUtils.buildErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getLocalizedMessage(), null);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        return handleExceptionInternal(ex, body, headers, HttpStatus.NOT_FOUND, request);
    }
}

package com.francelabs.datafari.rest.v2_0.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class FileUploadExceptionHandler extends ResponseEntityExceptionHandler {

  @Value("${spring.servlet.multipart.max-file-size}")
  String maxFileSize;

  @Value("${spring.servlet.multipart.max-request-size}")
  String maxRequestSize;

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<String> handleMaxSizeException(final MaxUploadSizeExceededException exc) {

    return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("One or more files are too large !" + System.lineSeparator() + "Max file size is set to: " + maxFileSize + System.lineSeparator()
        + "Max request size (total files) is set to: " + maxRequestSize);
  }

}

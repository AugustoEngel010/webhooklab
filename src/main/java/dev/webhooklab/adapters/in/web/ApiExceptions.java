package dev.webhooklab.adapters.in.web;

import dev.webhooklab.application.usecase.DeliveryConflictException;
import dev.webhooklab.application.usecase.EventNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptions {
  record ErrorResponse(int status, String code, String detail) {}

  @ExceptionHandler({
    IllegalArgumentException.class,
    MalformedUuidException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  ResponseEntity<ErrorResponse> badRequest(RuntimeException e) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse(400, "INVALID_REQUEST", "Request parameters or body are invalid"));
  }

  @ExceptionHandler({NotFoundException.class, EventNotFoundException.class})
  ResponseEntity<ErrorResponse> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(404, "EVENT_NOT_FOUND", "Event was not found"));
  }

  @ExceptionHandler(DeliveryConflictException.class)
  ResponseEntity<ErrorResponse> conflict(DeliveryConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "DELIVERY_CONFLICT", e.getMessage()));
  }

  @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
  ResponseEntity<ErrorResponse> persistenceFailure(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new ErrorResponse(503, "PERSISTENCE_FAILURE", "Event history could not be read"));
  }
}

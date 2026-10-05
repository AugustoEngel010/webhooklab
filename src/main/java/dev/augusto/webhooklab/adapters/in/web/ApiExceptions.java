package dev.augusto.webhooklab.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

class MalformedUuidException extends RuntimeException { }
class NotFoundException extends RuntimeException { }

@RestControllerAdvice
public class ApiExceptions {
    record ErrorResponse(int status, String code, String detail) { }

    @ExceptionHandler({IllegalArgumentException.class, MalformedUuidException.class})
    ResponseEntity<ErrorResponse> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "INVALID_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(404, "EVENT_NOT_FOUND", "Event was not found"));
    }
}

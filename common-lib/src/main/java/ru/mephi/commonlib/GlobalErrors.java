package ru.mephi.commonlib;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

record ErrorEnvelope(
    String code,
    String message,
    String requestId,
    Instant timestamp,
    Map<String, Object> details) {}

@RestControllerAdvice
public class GlobalErrors {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorEnvelope> badRequest(
      MethodArgumentNotValidException ex,
      @RequestHeader(value = "X-Request-Id", required = false) String rid) {
    return ResponseEntity.badRequest()
        .body(new ErrorEnvelope("VALIDATION_ERROR", ex.getMessage(), rid, Instant.now(), Map.of()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorEnvelope> conflict(
      IllegalStateException ex,
      @RequestHeader(value = "X-Request-Id", required = false) String rid) {
    return ResponseEntity.status(409)
        .body(new ErrorEnvelope(ex.getMessage(), ex.getMessage(), rid, Instant.now(), Map.of()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorEnvelope> generic(
      Exception ex, @RequestHeader(value = "X-Request-Id", required = false) String rid) {
    return ResponseEntity.status(503)
        .body(new ErrorEnvelope("SERVICE_ERROR", ex.getMessage(), rid, Instant.now(), Map.of()));
  }
}

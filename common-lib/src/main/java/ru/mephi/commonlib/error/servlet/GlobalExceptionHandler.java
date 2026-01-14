package ru.mephi.commonlib.error.servlet;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.mephi.commonlib.error.BusinessException;
import ru.mephi.commonlib.error.ErrorEnvelope;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String REQUEST_ID_HEADER = "X-Request-Id";

  private String extractRequestId(WebRequest webRequest) {
    return webRequest.getHeader(REQUEST_ID_HEADER);
  }

  private ResponseEntity<ErrorEnvelope> buildResponse(
      String requestId, HttpStatus httpStatus, String errorCode, String errorMessage, Map<String, Object> details) {
    ErrorEnvelope envelope = ErrorEnvelope.of(errorCode, errorMessage, requestId, details);
    return ResponseEntity.status(httpStatus).body(envelope);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorEnvelope> handleBusinessException(BusinessException exception, WebRequest webRequest) {
    String requestId = extractRequestId(webRequest);
    return buildResponse(requestId, exception.status(), exception.code(), exception.getMessage(), Map.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorEnvelope> handleValidationException(
      MethodArgumentNotValidException exception, WebRequest webRequest) {

    List<Map<String, String>> fieldErrors = exception.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(fieldError -> Map.of("field", fieldError.getField(), "message", fieldError.getDefaultMessage()))
        .toList();

    String requestId = extractRequestId(webRequest);
    return buildResponse(requestId, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", Map.of("errors", fieldErrors));
  }

  @ExceptionHandler({
      ConstraintViolationException.class,
      MissingServletRequestParameterException.class,
      MethodArgumentTypeMismatchException.class,
      HttpMessageNotReadableException.class
  })
  public ResponseEntity<ErrorEnvelope> handleBadRequest(Exception exception, WebRequest webRequest) {
    String requestId = extractRequestId(webRequest);
    return buildResponse(requestId, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage(), Map.of());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorEnvelope> handleAuthenticationException(AuthenticationException exception, WebRequest webRequest) {
    String requestId = extractRequestId(webRequest);
    return buildResponse(requestId, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage(), Map.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorEnvelope> handleAccessDeniedException(AccessDeniedException exception, WebRequest webRequest) {
    String requestId = extractRequestId(webRequest);
    return buildResponse(requestId, HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage(), Map.of());
  }

  @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
  public ResponseEntity<ErrorEnvelope> handleNotFoundException(RuntimeException exception, WebRequest webRequest) {
    String requestId = extractRequestId(webRequest);
    return buildResponse(requestId, HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), Map.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorEnvelope> handleDataIntegrityException(
      DataIntegrityViolationException exception, WebRequest webRequest) {

    String detailMessage;
    if (exception.getMostSpecificCause() != null) {
      detailMessage = exception.getMostSpecificCause().getMessage();
    } else {
      detailMessage = exception.getMessage();
    }

    String requestId = extractRequestId(webRequest);
    return buildResponse(requestId, HttpStatus.CONFLICT, "CONFLICT", "Data integrity violation", Map.of("detail", detailMessage));
  }

  @ExceptionHandler({TimeoutException.class, SocketTimeoutException.class})
  public ResponseEntity<ErrorEnvelope> handleTimeoutException(Exception exception, WebRequest webRequest) {
    String requestId = extractRequestId(webRequest);
    return buildResponse(requestId, HttpStatus.GATEWAY_TIMEOUT, "GATEWAY_TIMEOUT", "Upstream timeout", Map.of());
  }

  @ExceptionHandler({HttpStatusCodeException.class, WebClientResponseException.class})
  public ResponseEntity<ErrorEnvelope> handleUpstreamException(RuntimeException exception, WebRequest webRequest) {
    int upstreamStatusCode;
    String responseBody;

    if (exception instanceof HttpStatusCodeException httpException) {
      upstreamStatusCode = httpException.getStatusCode().value();
      responseBody = httpException.getResponseBodyAsString();
    } else {
      WebClientResponseException webClientException = (WebClientResponseException) exception;
      upstreamStatusCode = webClientException.getStatusCode().value();
      responseBody = webClientException.getResponseBodyAsString();
    }

    HttpStatus responseStatus;
    if (upstreamStatusCode >= 500) {
      responseStatus = HttpStatus.SERVICE_UNAVAILABLE;
    } else {
      responseStatus = HttpStatus.BAD_GATEWAY;
    }

    String requestId = extractRequestId(webRequest);
    return buildResponse(requestId, responseStatus, "SERVICE_ERROR", "Upstream error",
        Map.of("body", responseBody, "upstreamStatus", upstreamStatusCode));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorEnvelope> handleGenericException(Exception exception, WebRequest webRequest) {
    String requestId = extractRequestId(webRequest);
    return buildResponse(requestId, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_ERROR", exception.getMessage(), Map.of());
  }
}

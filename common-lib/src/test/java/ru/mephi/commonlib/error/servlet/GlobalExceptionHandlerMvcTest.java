package ru.mephi.commonlib.error.servlet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.net.SocketTimeoutException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.mephi.commonlib.error.BusinessException;
import ru.mephi.commonlib.error.ErrorEnvelope;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerMvcTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        lenient().when(webRequest.getHeader("X-Request-Id")).thenReturn("test-request-id");
    }

    @Test
    void handleBusinessExceptionReturnsCorrectStatus() {
        BusinessException exception = new BusinessException("CUSTOM_ERROR", "Custom message", HttpStatus.BAD_REQUEST);

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleBusinessException(exception, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleBusinessExceptionReturnsCorrectCode() {
        BusinessException exception = new BusinessException("MY_CODE", "Message", HttpStatus.OK);

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleBusinessException(exception, webRequest);

        assertEquals("MY_CODE", response.getBody().code());
    }

    @Test
    void handleBusinessExceptionReturnsCorrectMessage() {
        BusinessException exception = new BusinessException("CODE", "Detailed error message", HttpStatus.OK);

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleBusinessException(exception, webRequest);

        assertEquals("Detailed error message", response.getBody().message());
    }

    @Test
    void handleBusinessExceptionIncludesRequestId() {
        BusinessException exception = new BusinessException("CODE", "msg", HttpStatus.OK);

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleBusinessException(exception, webRequest);

        assertEquals("test-request-id", response.getBody().requestId());
    }

    @Test
    void handleBadRequestConstraintViolationReturns400() {
        ConstraintViolationException exception = new ConstraintViolationException("Constraint violated", null);

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleBadRequest(exception, webRequest);

        assertEquals("VALIDATION_ERROR", response.getBody().code());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleBadRequestMissingParameterReturns400() {
        MissingServletRequestParameterException exception =
            new MissingServletRequestParameterException("param", "String");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleBadRequest(exception, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleAuthenticationExceptionReturns401() {
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleAuthenticationException(exception, webRequest);

        assertEquals("UNAUTHORIZED", response.getBody().code());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleAccessDeniedExceptionReturns403() {
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleAccessDeniedException(exception, webRequest);

        assertEquals("FORBIDDEN", response.getBody().code());
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void handleNotFoundExceptionEntityNotFoundReturns404() {
        EntityNotFoundException exception = new EntityNotFoundException("Entity not found");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleNotFoundException(exception, webRequest);

        assertEquals("NOT_FOUND", response.getBody().code());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleNotFoundExceptionNoSuchElementReturns404() {
        NoSuchElementException exception = new NoSuchElementException("No such element");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleNotFoundException(exception, webRequest);

        assertEquals("NOT_FOUND", response.getBody().code());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleDataIntegrityExceptionReturns409() {
        DataIntegrityViolationException exception =
            new DataIntegrityViolationException("Duplicate key");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleDataIntegrityException(exception, webRequest);

        assertEquals("CONFLICT", response.getBody().code());
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleDataIntegrityExceptionIncludesDetailInBody() {
        DataIntegrityViolationException exception =
            new DataIntegrityViolationException("Unique constraint violated");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleDataIntegrityException(exception, webRequest);

        assertNotNull(response.getBody().details().get("detail"));
    }

    @Test
    void handleTimeoutExceptionReturns504() {
        TimeoutException exception = new TimeoutException("Request timed out");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleTimeoutException(exception, webRequest);

        assertEquals("GATEWAY_TIMEOUT", response.getBody().code());
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    }

    @Test
    void handleTimeoutExceptionSocketTimeoutReturns504() {
        SocketTimeoutException exception = new SocketTimeoutException("Socket timed out");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleTimeoutException(exception, webRequest);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    }

    @Test
    void handleUpstreamExceptionWebClient5xxReturns503() {
        WebClientResponseException exception = WebClientResponseException.create(
                500, "Internal Server Error", null, "error body".getBytes(), null);

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleUpstreamException(exception, webRequest);

        assertEquals("SERVICE_ERROR", response.getBody().code());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void handleUpstreamExceptionWebClient4xxReturns502() {
        WebClientResponseException exception = WebClientResponseException.create(
                400, "Bad Request", null, "bad request".getBytes(), null);

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleUpstreamException(exception, webRequest);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void handleUpstreamExceptionIncludesUpstreamInfo() {
        WebClientResponseException exception = WebClientResponseException.create(
                503, "Service Unavailable", null, "{\"error\":\"down\"}".getBytes(), null);

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleUpstreamException(exception, webRequest);

        assertNotNull(response.getBody().details().get("body"));
        assertEquals(503, response.getBody().details().get("upstreamStatus"));
    }

    @Test
    void handleGenericExceptionReturns503() {
        RuntimeException exception = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleGenericException(exception, webRequest);

        assertEquals("SERVICE_ERROR", response.getBody().code());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void handleGenericExceptionIncludesExceptionMessage() {
        RuntimeException exception = new RuntimeException("Something unexpected happened");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleGenericException(exception, webRequest);

        assertEquals("Something unexpected happened", response.getBody().message());
    }

    @Test
    void handlerHandlesNullRequestId() {
        when(webRequest.getHeader("X-Request-Id")).thenReturn(null);
        RuntimeException exception = new RuntimeException("error");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleGenericException(exception, webRequest);

        assertNull(response.getBody().requestId());
    }

    @Test
    void handlerHandlesEmptyRequestId() {
        when(webRequest.getHeader("X-Request-Id")).thenReturn("");
        RuntimeException exception = new RuntimeException("error");

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleGenericException(exception, webRequest);

        assertEquals("", response.getBody().requestId());
    }

    @Test
    void allResponsesIncludeTimestamp() {
        BusinessException exception = new BusinessException("CODE", "msg", HttpStatus.OK);

        ResponseEntity<ErrorEnvelope> response = exceptionHandler.handleBusinessException(exception, webRequest);

        assertNotNull(response.getBody().timestamp());
    }
}

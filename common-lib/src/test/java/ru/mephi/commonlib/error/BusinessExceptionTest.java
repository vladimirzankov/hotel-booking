package ru.mephi.commonlib.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructor_setsAllFields() {
        BusinessException ex = new BusinessException("ERR_001", "Something went wrong", HttpStatus.BAD_REQUEST);

        assertEquals("ERR_001", ex.code());
        assertEquals("Something went wrong", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.status());
    }

    @Test
    void code_returnsCorrectCode() {
        BusinessException ex = new BusinessException("CUSTOM_CODE", "message", HttpStatus.OK);

        assertEquals("CUSTOM_CODE", ex.code());
    }

    @Test
    void status_returnsCorrectStatus() {
        BusinessException ex = new BusinessException("CODE", "message", HttpStatus.NOT_FOUND);

        assertEquals(HttpStatus.NOT_FOUND, ex.status());
    }

    @Test
    void getMessage_returnsCorrectMessage() {
        BusinessException ex = new BusinessException("CODE", "Custom error message", HttpStatus.OK);

        assertEquals("Custom error message", ex.getMessage());
    }

    @Test
    void isRuntimeException() {
        BusinessException ex = new BusinessException("CODE", "msg", HttpStatus.OK);

        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void canBeThrown() {
        assertThrows(BusinessException.class, () -> {
            throw new BusinessException("ERROR", "Test throw", HttpStatus.INTERNAL_SERVER_ERROR);
        });
    }

    @Test
    void canBeCaught_asBusinessException() {
        try {
            throw new BusinessException("CAUGHT", "Caught message", HttpStatus.CONFLICT);
        } catch (BusinessException e) {
            assertEquals("CAUGHT", e.code());
            assertEquals("Caught message", e.getMessage());
            assertEquals(HttpStatus.CONFLICT, e.status());
        }
    }

    @Test
    void canBeCaught_asRuntimeException() {
        try {
            throw new BusinessException("RUNTIME", "Runtime catch", HttpStatus.OK);
        } catch (RuntimeException e) {
            assertInstanceOf(BusinessException.class, e);
        }
    }

    @Test
    void status_badRequest() {
        BusinessException ex = new BusinessException("BAD", "Bad request", HttpStatus.BAD_REQUEST);
        assertEquals(400, ex.status().value());
    }

    @Test
    void status_unauthorized() {
        BusinessException ex = new BusinessException("UNAUTH", "Unauthorized", HttpStatus.UNAUTHORIZED);
        assertEquals(401, ex.status().value());
    }

    @Test
    void status_forbidden() {
        BusinessException ex = new BusinessException("FORBID", "Forbidden", HttpStatus.FORBIDDEN);
        assertEquals(403, ex.status().value());
    }

    @Test
    void status_notFound() {
        BusinessException ex = new BusinessException("NOTFOUND", "Not found", HttpStatus.NOT_FOUND);
        assertEquals(404, ex.status().value());
    }

    @Test
    void status_conflict() {
        BusinessException ex = new BusinessException("CONFLICT", "Conflict", HttpStatus.CONFLICT);
        assertEquals(409, ex.status().value());
    }

    @Test
    void status_internalServerError() {
        BusinessException ex = new BusinessException("ISE", "Internal error", HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(500, ex.status().value());
    }

    @Test
    void status_serviceUnavailable() {
        BusinessException ex = new BusinessException("SVC", "Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        assertEquals(503, ex.status().value());
    }

    @Test
    void nullCode_isAllowed() {
        BusinessException ex = new BusinessException(null, "message", HttpStatus.OK);
        assertNull(ex.code());
    }

    @Test
    void emptyCode_isAllowed() {
        BusinessException ex = new BusinessException("", "message", HttpStatus.OK);
        assertEquals("", ex.code());
    }

    @Test
    void emptyMessage_isAllowed() {
        BusinessException ex = new BusinessException("CODE", "", HttpStatus.OK);
        assertEquals("", ex.getMessage());
    }
}

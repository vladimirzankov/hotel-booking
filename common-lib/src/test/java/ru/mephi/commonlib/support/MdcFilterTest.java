package ru.mephi.commonlib.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MdcFilterTest {

    private MdcFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new MdcFilter();
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_setsRequestId_whenPresent() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-123");

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        doAnswer(inv -> {
            capturedRequestId.set(MDC.get("requestId"));
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertEquals("req-123", capturedRequestId.get());
    }

    @Test
    void doFilterInternal_doesNotSetRequestId_whenNull() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        doAnswer(inv -> {
            capturedRequestId.set(MDC.get("requestId"));
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertNull(capturedRequestId.get());
    }

    @Test
    void doFilterInternal_doesNotSetRequestId_whenBlank() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("   ");

        AtomicReference<String> capturedRequestId = new AtomicReference<>();
        doAnswer(inv -> {
            capturedRequestId.set(MDC.get("requestId"));
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertNull(capturedRequestId.get());
    }

    @Test
    void doFilterInternal_setsUserId_whenAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, List.of())
        );

        AtomicReference<String> capturedUserId = new AtomicReference<>();
        doAnswer(inv -> {
            capturedUserId.set(MDC.get("userId"));
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertEquals("testuser", capturedUserId.get());
    }

    @Test
    void doFilterInternal_doesNotSetUserId_whenNotAuthenticated() throws Exception {
        AtomicReference<String> capturedUserId = new AtomicReference<>();
        doAnswer(inv -> {
            capturedUserId.set(MDC.get("userId"));
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertNull(capturedUserId.get());
    }

    @Test
    void doFilterInternal_setsBookingId_whenPresent() throws Exception {
        when(request.getHeader("X-Booking-Id")).thenReturn("booking-456");

        AtomicReference<String> capturedBookingId = new AtomicReference<>();
        doAnswer(inv -> {
            capturedBookingId.set(MDC.get("bookingId"));
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertEquals("booking-456", capturedBookingId.get());
    }

    @Test
    void doFilterInternal_doesNotSetBookingId_whenNull() throws Exception {
        when(request.getHeader("X-Booking-Id")).thenReturn(null);

        AtomicReference<String> capturedBookingId = new AtomicReference<>();
        doAnswer(inv -> {
            capturedBookingId.set(MDC.get("bookingId"));
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertNull(capturedBookingId.get());
    }

    @Test
    void doFilterInternal_clearsMdc_afterChain() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-123");
        when(request.getHeader("X-Booking-Id")).thenReturn("booking-123");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, List.of())
        );

        filter.doFilterInternal(request, response, chain);

        assertNull(MDC.get("requestId"));
        assertNull(MDC.get("userId"));
        assertNull(MDC.get("bookingId"));
    }

    @Test
    void doFilterInternal_clearsMdc_evenOnException() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-123");
        doThrow(new RuntimeException("Test exception")).when(chain).doFilter(request, response);

        assertThrows(RuntimeException.class, () -> 
            filter.doFilterInternal(request, response, chain)
        );

        assertNull(MDC.get("requestId"));
    }

    @Test
    void doFilterInternal_callsFilterChain() throws Exception {
        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_setsAllMdcValues_simultaneously() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("req-all");
        when(request.getHeader("X-Booking-Id")).thenReturn("booking-all");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alluser", null, List.of())
        );

        List<String> captured = new ArrayList<>();
        doAnswer(inv -> {
            captured.add(MDC.get("requestId"));
            captured.add(MDC.get("userId"));
            captured.add(MDC.get("bookingId"));
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertEquals("req-all", captured.get(0));
        assertEquals("alluser", captured.get(1));
        assertEquals("booking-all", captured.get(2));
    }
}

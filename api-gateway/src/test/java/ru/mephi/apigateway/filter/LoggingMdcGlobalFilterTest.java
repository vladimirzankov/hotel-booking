package ru.mephi.apigateway.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class LoggingMdcGlobalFilterTest {

    private LoggingMdcGlobalFilter mdcFilter;
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        mdcFilter = new LoggingMdcGlobalFilter();
        filterChain = mock(GatewayFilterChain.class);
        MDC.clear();
    }

    @Test
    void filterSetsRequestIdWhenPresent() {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.get("/test")
                .header("X-Request-Id", "req-123")
                .build();
        MockServerWebExchange webExchange = MockServerWebExchange.from(httpRequest);

        AtomicReference<String> capturedId = new AtomicReference<>();
        when(filterChain.filter(any())).thenAnswer(invocation -> {
            capturedId.set(MDC.get("requestId"));
            return Mono.empty();
        });

        mdcFilter.filter(webExchange, filterChain).block();

        assertEquals("req-123", capturedId.get());
    }

    @Test
    void filterDoesNotSetRequestIdWhenMissing() {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange webExchange = MockServerWebExchange.from(httpRequest);

        AtomicReference<String> capturedId = new AtomicReference<>();
        when(filterChain.filter(any())).thenAnswer(invocation -> {
            capturedId.set(MDC.get("requestId"));
            return Mono.empty();
        });

        mdcFilter.filter(webExchange, filterChain).block();

        assertNull(capturedId.get());
    }

    @Test
    void filterClearsMdcAfterCompletion() {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.get("/test")
                .header("X-Request-Id", "req-456")
                .build();
        MockServerWebExchange webExchange = MockServerWebExchange.from(httpRequest);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        mdcFilter.filter(webExchange, filterChain).block();

        assertNull(MDC.get("requestId"));
    }

    @Test
    void filterClearsMdcOnError() {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.get("/test")
                .header("X-Request-Id", "req-error")
                .build();
        MockServerWebExchange webExchange = MockServerWebExchange.from(httpRequest);

        when(filterChain.filter(any())).thenReturn(Mono.error(new RuntimeException("Test error")));

        try {
            mdcFilter.filter(webExchange, filterChain).block();
        } catch (Exception ignored) {
        }

        assertNull(MDC.get("requestId"));
    }

    @Test
    void filterCallsChain() {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange webExchange = MockServerWebExchange.from(httpRequest);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        mdcFilter.filter(webExchange, filterChain).block();

        verify(filterChain).filter(webExchange);
    }

    @Test
    void getOrderReturnsNegative50() {
        assertEquals(-50, mdcFilter.getOrder());
    }

    @Test
    void getOrderIsAfterRequestIdFilter() {
        RequestIdGatewayFilter requestIdFilter = new RequestIdGatewayFilter();
        assertTrue(mdcFilter.getOrder() > requestIdFilter.getOrder());
    }

    @Test
    void filterHandlesEmptyRequestId() {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.get("/test")
                .header("X-Request-Id", "")
                .build();
        MockServerWebExchange webExchange = MockServerWebExchange.from(httpRequest);

        AtomicReference<String> capturedId = new AtomicReference<>();
        when(filterChain.filter(any())).thenAnswer(invocation -> {
            capturedId.set(MDC.get("requestId"));
            return Mono.empty();
        });

        mdcFilter.filter(webExchange, filterChain).block();

        assertEquals("", capturedId.get());
    }

    @Test
    void filterPreservesRequestIdThroughChain() {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.get("/test")
                .header("X-Request-Id", "preserve-me")
                .build();
        MockServerWebExchange webExchange = MockServerWebExchange.from(httpRequest);

        AtomicReference<String> startId = new AtomicReference<>();
        AtomicReference<String> endId = new AtomicReference<>();

        when(filterChain.filter(any())).thenAnswer(invocation -> {
            startId.set(MDC.get("requestId"));
            return Mono.defer(() -> {
                endId.set(MDC.get("requestId"));
                return Mono.empty();
            });
        });

        mdcFilter.filter(webExchange, filterChain).block();

        assertEquals("preserve-me", endId.get());
        assertEquals("preserve-me", startId.get());
    }
}

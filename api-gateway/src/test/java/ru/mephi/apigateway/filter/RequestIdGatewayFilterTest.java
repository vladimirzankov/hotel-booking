package ru.mephi.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RequestIdGatewayFilterTest {

    private RequestIdGatewayFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestIdGatewayFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_addsRequestId_whenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange swe = (ServerWebExchange) ex;
            String requestId = swe.getRequest().getHeaders().getFirst("X-Request-Id");
            return requestId != null && !requestId.isBlank();
        }));
    }

    @Test
    void filter_generatesUuidFormat() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange swe = (ServerWebExchange) ex;
            String requestId = swe.getRequest().getHeaders().getFirst("X-Request-Id");
            return requestId != null && 
                   requestId.length() == 36 &&
                   requestId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }));
    }

    @Test
    void filter_doesNotAddRequestId_whenPresent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Request-Id", "existing-id")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange swe = (ServerWebExchange) ex;
            return "existing-id".equals(swe.getRequest().getHeaders().getFirst("X-Request-Id"));
        }));
    }

    @Test
    void filter_removesCookieHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(HttpHeaders.COOKIE, "session=abc123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange swe = (ServerWebExchange) ex;
            return !swe.getRequest().getHeaders().containsKey(HttpHeaders.COOKIE);
        }));
    }

    @Test
    void filter_removesCookie_whenRequestIdExists() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-Request-Id", "existing")
                .header(HttpHeaders.COOKIE, "session=abc123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange swe = (ServerWebExchange) ex;
            return !swe.getRequest().getHeaders().containsKey(HttpHeaders.COOKIE);
        }));
    }

    @Test
    void filter_preservesOtherHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("Authorization", "Bearer token")
                .header("Content-Type", "application/json")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain).filter(argThat(ex -> {
            ServerWebExchange swe = (ServerWebExchange) ex;
            HttpHeaders headers = swe.getRequest().getHeaders();
            return "Bearer token".equals(headers.getFirst("Authorization")) &&
                   "application/json".equals(headers.getFirst("Content-Type"));
        }));
    }

    @Test
    void getOrder_returnsNegative100() {
        assertEquals(-100, filter.getOrder());
    }

    @Test
    void getOrder_isHighPriority() {
        assertTrue(filter.getOrder() < 0);
    }

    @Test
    void filter_callsChain() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(any());
    }

    @Test
    void filter_generatesUniqueIds() {
        MockServerHttpRequest request1 = MockServerHttpRequest.get("/test1").build();
        MockServerHttpRequest request2 = MockServerHttpRequest.get("/test2").build();
        MockServerWebExchange exchange1 = MockServerWebExchange.from(request1);
        MockServerWebExchange exchange2 = MockServerWebExchange.from(request2);

        final String[] ids = new String[2];

        when(chain.filter(any())).thenAnswer(inv -> {
            ServerWebExchange ex = inv.getArgument(0);
            String id = ex.getRequest().getHeaders().getFirst("X-Request-Id");
            if (ids[0] == null) ids[0] = id;
            else ids[1] = id;
            return Mono.empty();
        });

        filter.filter(exchange1, chain).block();
        filter.filter(exchange2, chain).block();

        assertNotEquals(ids[0], ids[1]);
    }
}

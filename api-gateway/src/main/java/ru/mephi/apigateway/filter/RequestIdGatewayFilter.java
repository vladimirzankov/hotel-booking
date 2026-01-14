package ru.mephi.apigateway.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestIdGatewayFilter implements GatewayFilter, Ordered {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final int FILTER_ORDER = -100;

  @Override
  public int getOrder() {
    return FILTER_ORDER;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain filterChain) {
    HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
    boolean hasRequestId = requestHeaders.containsKey(REQUEST_ID_HEADER);

    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
    requestBuilder.headers(headers -> headers.remove(HttpHeaders.COOKIE));

    if (hasRequestId == false) {
      String generatedId = UUID.randomUUID().toString();
      requestBuilder.header(REQUEST_ID_HEADER, generatedId);
    }

    ServerHttpRequest modifiedRequest = requestBuilder.build();
    ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();

    return filterChain.filter(modifiedExchange);
  }
}

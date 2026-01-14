package ru.mephi.apigateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingMdcGlobalFilter implements GlobalFilter, Ordered {

  private static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String MDC_REQUEST_ID = "requestId";
  private static final int FILTER_ORDER = -50;

  @Override
  public int getOrder() {
    return FILTER_ORDER;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain filterChain) {
    ServerHttpRequest httpRequest = exchange.getRequest();
    String requestId = httpRequest.getHeaders().getFirst(REQUEST_ID_HEADER);

    if (requestId == null) {
      return filterChain.filter(exchange).doFinally(signal -> MDC.clear());
    }

    MDC.put(MDC_REQUEST_ID, requestId);
    return filterChain.filter(exchange).doFinally(signal -> MDC.clear());
  }
}

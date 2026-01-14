package ru.mephi.commonlib.error.reactive;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Order(-2)
public class JsonErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

  public JsonErrorWebExceptionHandler(
      GlobalErrorAttributes errorAttributes,
      WebProperties webProperties,
      ApplicationContext applicationContext,
      ServerCodecConfigurer serverCodecConfigurer) {

    super(errorAttributes, webProperties.getResources(), applicationContext);
    super.setMessageWriters(serverCodecConfigurer.getWriters());
    super.setMessageReaders(serverCodecConfigurer.getReaders());
  }

  @Override
  protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
    return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
  }

  private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
    var opts = ErrorAttributeOptions.defaults();
    Map<String, Object> attrs = new LinkedHashMap<>(getErrorAttributes(request, opts));

    int status = (int) attrs.getOrDefault("status", 503);
    attrs.remove("status"); // HTTP-код уйдёт в статус ответа

    return ServerResponse.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(attrs));
  }
}

package ru.mephi.apigateway.config;

import ru.mephi.commonlib.error.reactive.GlobalErrorAttributes;
import ru.mephi.commonlib.error.reactive.JsonErrorWebExceptionHandler;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;

@Configuration
public class ReactiveErrorHandlingConfig {

  @Bean
  public GlobalErrorAttributes globalErrorAttributes() {
    return new GlobalErrorAttributes();
  }

  @Bean
  public WebProperties webProperties() {
    return new WebProperties();
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public ErrorWebExceptionHandler jsonErrorWebExceptionHandler(
      GlobalErrorAttributes attributes,
      WebProperties webProperties,
      ApplicationContext applicationContext,
      ServerCodecConfigurer serverCodecConfigurer) {
    return new JsonErrorWebExceptionHandler(
        attributes, webProperties, applicationContext, serverCodecConfigurer);
  }
}

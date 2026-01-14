package ru.mephi.apigateway.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@EnableReactiveMethodSecurity
@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String DEFAULT_ROLE = "ROLE_USER";

  @Bean
  ReactiveJwtDecoder gatewayJwtDecoder(@Value("${auth.jwt.secret}") String jwtSecret) {
    byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    SecretKey secretKey = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
    return NimbusReactiveJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> reactiveJwtAuthConverter() {
    JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
    jwtConverter.setJwtGrantedAuthoritiesConverter(token -> {
      Object roleValue = token.getClaims().getOrDefault("role", DEFAULT_ROLE);
      String role = (String) roleValue;
      return List.of(new SimpleGrantedAuthority(role));
    });
    return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
  }

  @Bean
  SecurityWebFilterChain springSecurity(
      ServerHttpSecurity http,
      Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthConverter,
      ReactiveJwtDecoder jwtDecoder) {

    http.csrf(ServerHttpSecurity.CsrfSpec::disable);

    http.authorizeExchange(exchanges -> exchanges
        .pathMatchers("/user/register", "/user/auth").permitAll()
        .pathMatchers("/actuator/**").permitAll()
        .anyExchange().authenticated()
    );

    http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtSpec -> {
      jwtSpec.jwtDecoder(jwtDecoder);
      jwtSpec.jwtAuthenticationConverter(jwtAuthConverter);
    }));

    return http.build();
  }
}

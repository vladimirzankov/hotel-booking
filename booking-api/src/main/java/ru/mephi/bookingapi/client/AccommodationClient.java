package ru.mephi.bookingapi.client;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Component
public class AccommodationClient {
  private final WebClient client;
  private final Retry retry;

  public AccommodationClient(
      RemoteClientConfig p, @Value("${hotel.base-url:http://localhost:8081}") String baseUrl) {
    this.client =
        WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(
                new ReactorClientHttpConnector(
                    HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, p.connectTimeoutMs())
                        .responseTimeout(Duration.ofMillis(p.readTimeoutMs()))))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    this.retry =
        Retry.backoff(p.retries(), Duration.ofMillis(p.backoffMs()))
            .maxBackoff(Duration.ofSeconds(1));
  }

  public void confirm(String token, Long roomId, String requestId, String start, String end) {
    client
        .post()
        .uri("/internal/rooms/{id}/confirm-availability", roomId)
        .header(HttpHeaders.AUTHORIZATION, token)
        .header("X-Request-Id", requestId)
        .bodyValue(new Confirm(start, end, requestId))
        .retrieve()
        .toBodilessEntity()
        .retryWhen(retry)
        .block();
  }

  public List<?> recommend(String token, Long hotelId, String start, String end, int limit) {
    return client
        .get()
        .uri(
            uri ->
                uri.path("/api/rooms/recommend")
                    .queryParam("hotelId", hotelId)
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("limit", limit)
                    .build())
        .header(HttpHeaders.AUTHORIZATION, token)
        .retrieve()
        .bodyToFlux(Object.class)
        .collectList()
        .block(Duration.ofSeconds(2));
  }

  public void release(String token, Long roomId, String requestId) {
    client
        .post()
        .uri(uri -> uri.path("/internal/rooms/{id}/release").build(roomId))
        .header(HttpHeaders.AUTHORIZATION, token)
        .header("X-Request-Id", requestId)
        .retrieve()
        .toBodilessEntity()
        .block(Duration.ofSeconds(2));
  }

  private record Confirm(String start, String end, String requestId) {}
}

package ru.mephi.bookingapi.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hotel.client")
public record RemoteClientConfig(
    int connectTimeoutMs, int readTimeoutMs, int retries, int backoffMs) {}

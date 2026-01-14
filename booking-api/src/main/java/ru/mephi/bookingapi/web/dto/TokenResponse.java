package ru.mephi.bookingapi.web.dto;

public record TokenResponse(String token, long expiresInSeconds) {}

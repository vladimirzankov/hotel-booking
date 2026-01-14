package ru.mephi.bookingapi.web.dto;

public record RegistrationRequest(String username, String password, boolean admin) {}

package ru.mephi.bookingapi.web.dto;

public record NewUserRequest(String username, String password, String role) {}

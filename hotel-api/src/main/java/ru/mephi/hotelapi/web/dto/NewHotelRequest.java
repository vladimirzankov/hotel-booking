package ru.mephi.hotelapi.web.dto;

import jakarta.validation.constraints.NotBlank;

public record NewHotelRequest(@NotBlank String name, String city) {}

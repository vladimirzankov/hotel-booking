package ru.mephi.hotelapi.web.dto;

import jakarta.validation.constraints.NotNull;

public record NewRoomRequest(@NotNull Long hotelId, String number, Boolean available) {}

package ru.mephi.bookingapi.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record BookingDetails(
    Long id,
    Long userId,
    Long roomId,
    String status,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate startDate,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate endDate) {}

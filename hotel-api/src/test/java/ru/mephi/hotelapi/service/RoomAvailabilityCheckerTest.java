package ru.mephi.hotelapi.service;

import ru.mephi.hotelapi.domain.RoomReservation;
import ru.mephi.hotelapi.repo.RoomReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomAvailabilityCheckerTest {

    @Mock
    private RoomReservationRepository holds;

    @InjectMocks
    private RoomAvailabilityChecker availabilityService;

    private static final Long ROOM_ID = 1L;

    @BeforeEach
    void setUp() {
        reset(holds);
    }

    @Test
    void isAvailable_returnsTrue_whenNoHoldsExist() {
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end = LocalDate.of(2025, 10, 5);

        when(holds.findIntersecting(ROOM_ID, start, end)).thenReturn(Collections.emptyList());

        boolean result = availabilityService.isAvailable(ROOM_ID, start, end);

        assertTrue(result);
        verify(holds).findIntersecting(ROOM_ID, start, end);
    }

    @Test
    void isAvailable_returnsFalse_whenHoldExists() {
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end = LocalDate.of(2025, 10, 5);

        RoomReservation existingHold = RoomReservation.builder()
                .id(1L)
                .roomId(ROOM_ID)
                .requestId("req-1")
                .startDate(LocalDate.of(2025, 10, 2))
                .endDate(LocalDate.of(2025, 10, 4))
                .status(RoomReservation.Status.COMMITTED)
                .build();

        when(holds.findIntersecting(ROOM_ID, start, end)).thenReturn(List.of(existingHold));

        boolean result = availabilityService.isAvailable(ROOM_ID, start, end);

        assertFalse(result);
    }

    @Test
    void isAvailable_returnsFalse_whenMultipleHoldsExist() {
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end = LocalDate.of(2025, 10, 10);

        RoomReservation hold1 = RoomReservation.builder()
                .id(1L)
                .roomId(ROOM_ID)
                .requestId("req-1")
                .startDate(LocalDate.of(2025, 10, 2))
                .endDate(LocalDate.of(2025, 10, 4))
                .status(RoomReservation.Status.COMMITTED)
                .build();

        RoomReservation hold2 = RoomReservation.builder()
                .id(2L)
                .roomId(ROOM_ID)
                .requestId("req-2")
                .startDate(LocalDate.of(2025, 10, 6))
                .endDate(LocalDate.of(2025, 10, 8))
                .status(RoomReservation.Status.HELD)
                .build();

        when(holds.findIntersecting(ROOM_ID, start, end)).thenReturn(List.of(hold1, hold2));

        boolean result = availabilityService.isAvailable(ROOM_ID, start, end);

        assertFalse(result);
    }

    @Test
    void isAvailable_checksCorrectRoom() {
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end = LocalDate.of(2025, 10, 5);
        Long differentRoomId = 999L;

        when(holds.findIntersecting(differentRoomId, start, end)).thenReturn(Collections.emptyList());

        boolean result = availabilityService.isAvailable(differentRoomId, start, end);

        assertTrue(result);
        verify(holds).findIntersecting(differentRoomId, start, end);
        verify(holds, never()).findIntersecting(eq(ROOM_ID), any(), any());
    }

    @Test
    void isAvailable_checksCorrectDateRange() {
        LocalDate start = LocalDate.of(2025, 12, 15);
        LocalDate end = LocalDate.of(2025, 12, 20);

        when(holds.findIntersecting(ROOM_ID, start, end)).thenReturn(Collections.emptyList());

        availabilityService.isAvailable(ROOM_ID, start, end);

        verify(holds).findIntersecting(ROOM_ID, start, end);
    }

    @Test
    void isAvailable_returnsFalse_whenHoldIsExactlyTheSamePeriod() {
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end = LocalDate.of(2025, 10, 5);

        RoomReservation exactMatch = RoomReservation.builder()
                .id(1L)
                .roomId(ROOM_ID)
                .requestId("req-exact")
                .startDate(start)
                .endDate(end)
                .status(RoomReservation.Status.COMMITTED)
                .build();

        when(holds.findIntersecting(ROOM_ID, start, end)).thenReturn(List.of(exactMatch));

        boolean result = availabilityService.isAvailable(ROOM_ID, start, end);

        assertFalse(result);
    }

    @Test
    void isAvailable_singleDayBooking() {
        LocalDate date = LocalDate.of(2025, 10, 1);

        when(holds.findIntersecting(ROOM_ID, date, date)).thenReturn(Collections.emptyList());

        boolean result = availabilityService.isAvailable(ROOM_ID, date, date);

        assertTrue(result);
        verify(holds).findIntersecting(ROOM_ID, date, date);
    }
}

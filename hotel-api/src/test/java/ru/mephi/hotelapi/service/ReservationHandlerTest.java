package ru.mephi.hotelapi.service;

import ru.mephi.hotelapi.domain.Room;
import ru.mephi.hotelapi.domain.RoomReservation;
import ru.mephi.hotelapi.repo.RoomReservationRepository;
import ru.mephi.hotelapi.repo.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationHandlerTest {

    @Mock
    private RoomReservationRepository holds;

    @Mock
    private RoomRepository rooms;

    @InjectMocks
    private ReservationHandler holdService;

    private static final Long ROOM_ID = 1L;
    private static final String REQUEST_ID = "req-123";

    @BeforeEach
    void setUp() {
        reset(holds, rooms);
    }

    @Test
    void confirm_createsNewHold_whenNoExisting() {
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end = LocalDate.of(2025, 10, 5);

        when(holds.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(holds.findIntersectingForUpdate(ROOM_ID, start, end)).thenReturn(Collections.emptyList());
        when(rooms.findById(ROOM_ID)).thenReturn(Optional.of(Room.builder()
                .id(ROOM_ID)
                .hotelId(1L)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build()));

        RoomReservation savedHold = RoomReservation.builder()
                .id(1L)
                .roomId(ROOM_ID)
                .requestId(REQUEST_ID)
                .startDate(start)
                .endDate(end)
                .status(RoomReservation.Status.COMMITTED)
                .build();
        when(holds.save(any(RoomReservation.class))).thenReturn(savedHold);

        RoomReservation result = holdService.confirm(ROOM_ID, REQUEST_ID, start, end);

        assertNotNull(result);
        assertEquals(ROOM_ID, result.getRoomId());
        assertEquals(REQUEST_ID, result.getRequestId());
        assertEquals(RoomReservation.Status.COMMITTED, result.getStatus());
    }

    @Test
    void confirm_returnsExistingHold_whenIdempotent() {
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end = LocalDate.of(2025, 10, 5);

        RoomReservation existingHold = RoomReservation.builder()
                .id(1L)
                .roomId(ROOM_ID)
                .requestId(REQUEST_ID)
                .startDate(start)
                .endDate(end)
                .status(RoomReservation.Status.COMMITTED)
                .build();

        when(holds.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existingHold));

        RoomReservation result = holdService.confirm(ROOM_ID, REQUEST_ID, start, end);

        assertEquals(existingHold, result);
        verify(holds, never()).save(any());
        verify(holds, never()).findIntersectingForUpdate(any(), any(), any());
    }

    @Test
    void confirm_throwsException_whenRoomNotAvailable() {
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end = LocalDate.of(2025, 10, 5);

        when(holds.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        
        RoomReservation conflictingHold = RoomReservation.builder()
                .id(99L)
                .roomId(ROOM_ID)
                .requestId("other-request")
                .startDate(start)
                .endDate(end)
                .status(RoomReservation.Status.COMMITTED)
                .build();
        when(holds.findIntersectingForUpdate(ROOM_ID, start, end)).thenReturn(List.of(conflictingHold));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> holdService.confirm(ROOM_ID, REQUEST_ID, start, end));

        assertEquals("ROOM_NOT_AVAILABLE", exception.getMessage());
        verify(holds, never()).save(any());
    }

    @Test
    void confirm_incrementsTimesBooked() {
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end = LocalDate.of(2025, 10, 5);

        when(holds.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(holds.findIntersectingForUpdate(ROOM_ID, start, end)).thenReturn(Collections.emptyList());
        
        Room room = Room.builder()
                .id(ROOM_ID)
                .hotelId(1L)
                .number("101")
                .available(true)
                .timesBooked(5)
                .build();
        when(rooms.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(holds.save(any(RoomReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        holdService.confirm(ROOM_ID, REQUEST_ID, start, end);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(rooms).save(roomCaptor.capture());
        assertEquals(6, roomCaptor.getValue().getTimesBooked());
    }

    @Test
    void confirm_createsHoldWithCorrectStatus() {
        LocalDate start = LocalDate.of(2025, 10, 1);
        LocalDate end = LocalDate.of(2025, 10, 5);

        when(holds.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(holds.findIntersectingForUpdate(ROOM_ID, start, end)).thenReturn(Collections.emptyList());
        when(rooms.findById(ROOM_ID)).thenReturn(Optional.of(Room.builder()
                .id(ROOM_ID)
                .hotelId(1L)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build()));
        when(holds.save(any(RoomReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        holdService.confirm(ROOM_ID, REQUEST_ID, start, end);

        ArgumentCaptor<RoomReservation> holdCaptor = ArgumentCaptor.forClass(RoomReservation.class);
        verify(holds).save(holdCaptor.capture());
        
        RoomReservation savedHold = holdCaptor.getValue();
        assertEquals(RoomReservation.Status.COMMITTED, savedHold.getStatus());
        assertEquals(ROOM_ID, savedHold.getRoomId());
        assertEquals(REQUEST_ID, savedHold.getRequestId());
        assertEquals(start, savedHold.getStartDate());
        assertEquals(end, savedHold.getEndDate());
    }

    @Test
    void release_updatesStatus_whenHoldExists() {
        RoomReservation existingHold = RoomReservation.builder()
                .id(1L)
                .roomId(ROOM_ID)
                .requestId(REQUEST_ID)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build();

        when(holds.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(existingHold));

        holdService.release(ROOM_ID, REQUEST_ID);

        verify(holds).updateStatusByRequest(REQUEST_ID, ROOM_ID, RoomReservation.Status.RELEASED);
    }

    @Test
    void release_doesNothing_whenHoldNotFound() {
        when(holds.findByRequestId(REQUEST_ID)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> holdService.release(ROOM_ID, REQUEST_ID));

        verify(holds, never()).updateStatusByRequest(any(), any(), any());
    }

    @Test
    void release_doesNothing_whenAlreadyReleased() {
        RoomReservation releasedHold = RoomReservation.builder()
                .id(1L)
                .roomId(ROOM_ID)
                .requestId(REQUEST_ID)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.RELEASED)
                .build();

        when(holds.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(releasedHold));

        holdService.release(ROOM_ID, REQUEST_ID);

        verify(holds, never()).updateStatusByRequest(any(), any(), any());
    }

    @Test
    void release_releasesHeldStatus() {
        RoomReservation heldHold = RoomReservation.builder()
                .id(1L)
                .roomId(ROOM_ID)
                .requestId(REQUEST_ID)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.HELD)
                .build();

        when(holds.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(heldHold));

        holdService.release(ROOM_ID, REQUEST_ID);

        verify(holds).updateStatusByRequest(REQUEST_ID, ROOM_ID, RoomReservation.Status.RELEASED);
    }

    @Test
    void release_releasesCommittedStatus() {
        RoomReservation committedHold = RoomReservation.builder()
                .id(1L)
                .roomId(ROOM_ID)
                .requestId(REQUEST_ID)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build();

        when(holds.findByRequestId(REQUEST_ID)).thenReturn(Optional.of(committedHold));

        holdService.release(ROOM_ID, REQUEST_ID);

        verify(holds).updateStatusByRequest(REQUEST_ID, ROOM_ID, RoomReservation.Status.RELEASED);
    }
}

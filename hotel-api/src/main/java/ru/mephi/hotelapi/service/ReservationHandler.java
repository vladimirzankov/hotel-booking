package ru.mephi.hotelapi.service;

import ru.mephi.hotelapi.domain.Room;
import ru.mephi.hotelapi.domain.RoomReservation;
import ru.mephi.hotelapi.repo.RoomReservationRepository;
import ru.mephi.hotelapi.repo.RoomRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationHandler {
  private final RoomReservationRepository reservations;
  private final RoomRepository rooms;

  @Transactional
  public RoomReservation confirm(Long roomId, String requestId, LocalDate start, LocalDate end) {
    var existing = reservations.findByRequestId(requestId);
    if (existing.isPresent()) return existing.get();
    var locked = reservations.findIntersectingForUpdate(roomId, start, end);
    if (!locked.isEmpty()) throw new IllegalStateException("ROOM_NOT_AVAILABLE");
    RoomReservation saved =
        reservations.save(
            RoomReservation.builder()
                .roomId(roomId)
                .requestId(requestId)
                .startDate(start)
                .endDate(end)
                .status(RoomReservation.Status.COMMITTED)
                .build());
    Room r = rooms.findById(roomId).orElseThrow();
    r.setTimesBooked(r.getTimesBooked() + 1);
    rooms.save(r);
    return saved;
  }

  @Transactional
  public void release(Long roomId, String requestId) {
    reservations
        .findByRequestId(requestId)
        .ifPresent(
            h -> {
              if (h.getStatus() != RoomReservation.Status.RELEASED) {
                reservations.updateStatusByRequest(requestId, roomId, RoomReservation.Status.RELEASED);
              }
            });
  }
}

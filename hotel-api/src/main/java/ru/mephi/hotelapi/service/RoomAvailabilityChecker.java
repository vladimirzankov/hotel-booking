package ru.mephi.hotelapi.service;

import ru.mephi.hotelapi.domain.RoomReservation;
import ru.mephi.hotelapi.repo.RoomReservationRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomAvailabilityChecker {
  private final RoomReservationRepository reservations;

  public boolean isAvailable(Long roomId, LocalDate start, LocalDate end) {
    List<RoomReservation> xs = reservations.findIntersecting(roomId, start, end);
    return xs.isEmpty();
  }
}

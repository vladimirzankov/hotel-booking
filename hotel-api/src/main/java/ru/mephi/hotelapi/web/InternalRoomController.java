package ru.mephi.hotelapi.web;

import ru.mephi.hotelapi.domain.RoomReservation;
import ru.mephi.hotelapi.service.ReservationHandler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

record ConfirmRequest(
    @NotBlank String requestId, @NotNull LocalDate start, @NotNull LocalDate end) {}

@RestController
@RequestMapping("/internal/rooms")
@RequiredArgsConstructor
public class InternalRoomController {
  private final ReservationHandler reservationHandler;

  @PostMapping("/{id}/confirm-availability")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<RoomReservation> confirm(
      @RequestHeader("X-Request-Id") String requestId,
      @PathVariable Long id,
      @RequestBody ConfirmRequest req) {
    RoomReservation h = reservationHandler.confirm(id, requestId, req.start(), req.end());
    return ResponseEntity.ok(h);
  }

  @PostMapping("/{id}/release")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> release(
      @RequestHeader("X-Request-Id") String requestId, @PathVariable Long id) {
    reservationHandler.release(id, requestId);
    return ResponseEntity.accepted().build();
  }
}

package ru.mephi.bookingapi.web;

import ru.mephi.bookingapi.client.AccommodationClient;
import ru.mephi.bookingapi.config.TokenProvider;
import ru.mephi.bookingapi.domain.Booking;
import ru.mephi.bookingapi.repo.BookingRepository;
import ru.mephi.bookingapi.repo.UserRepository;
import ru.mephi.bookingapi.web.dto.*;
import java.time.Duration;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BookingController {
  private static final long INTERNAL_TOKEN_TTL_SECONDS = Duration.ofMinutes(5).toSeconds();
  private final BookingRepository bookings;
  private final UserRepository users;
  private final AccommodationClient accommodationClient;
  private final TokenProvider tokenProvider;

  @PostMapping("/booking")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<BookingResult> create(
      @RequestHeader("Authorization") String bearer,
      @RequestHeader("X-Request-Id") String requestId,
      @RequestBody CreateBookingRequest req,
      Principal principal) {
    var existing = bookings.findByRequestId(requestId);
    if (existing.isPresent()) {
      var b = existing.get();
      return ResponseEntity.ok(new BookingResult(b.getId(), b.getStatus().name(), b.getRoomId()));
    }
    MDC.put("requestId", requestId);
    
    Long userId = users.findByUsername(principal.getName())
        .map(u -> u.getId())
        .orElse(0L);
    
    var b =
        bookings.save(
            Booking.builder()
                .userId(userId)
                .status(Booking.Status.PENDING)
                .startDate(req.start())
                .endDate(req.end())
                .requestId(requestId)
                .build());
    Long roomId = req.roomId();
    try {
      if (req.autoSelect()) {
        var rec =
            accommodationClient.recommend(bearer, req.hotelId(), req.start().toString(), req.end().toString(), 1);
        if (rec.isEmpty()) throw new IllegalStateException("NO_RECOMMENDATION");
        roomId = Long.valueOf(((Map<?, ?>) rec.get(0)).get("id").toString());
      }
      if (roomId == null) throw new IllegalArgumentException("ROOM_ID_REQUIRED");
      String serviceBearer = "Bearer " + tokenProvider.issue("booking-api", "ROLE_ADMIN", INTERNAL_TOKEN_TTL_SECONDS);
      accommodationClient.confirm(serviceBearer, roomId, requestId, req.start().toString(), req.end().toString());
      b.setRoomId(roomId);
      b.setStatus(Booking.Status.CONFIRMED);
      bookings.save(b);
      return ResponseEntity.ok(new BookingResult(b.getId(), b.getStatus().name(), b.getRoomId()));
    } catch (Exception e) {
      if (roomId != null) {
        try {
          String serviceBearer = "Bearer " + tokenProvider.issue("booking-api", "ROLE_ADMIN", INTERNAL_TOKEN_TTL_SECONDS);
          accommodationClient.release(serviceBearer, roomId, requestId);
        } catch (Exception ignored) {
        }
      }
      b.setStatus(Booking.Status.CANCELLED);
      bookings.save(b);
      return ResponseEntity.status(503)
          .body(new BookingResult(b.getId(), b.getStatus().name(), roomId));
    } finally {
      MDC.clear();
    }
  }

  @GetMapping("/bookings")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<List<BookingDetails>> listMyBookings(Principal principal) {
    Long userId = users.findByUsername(principal.getName())
        .map(u -> u.getId())
        .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
    
    List<BookingDetails> result = bookings.findByUserId(userId).stream()
        .map(b -> new BookingDetails(
            b.getId(),
            b.getUserId(),
            b.getRoomId(),
            b.getStatus().name(),
            b.getStartDate(),
            b.getEndDate()))
        .toList();
    
    return ResponseEntity.ok(result);
  }

  @GetMapping("/booking/{id}")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<BookingDetails> getById(
      @PathVariable Long id,
      Principal principal) {
    Long userId = users.findByUsername(principal.getName())
        .map(u -> u.getId())
        .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
    
    Booking booking = bookings.findById(id)
        .orElseThrow(() -> new RuntimeException("BOOKING_NOT_FOUND"));
    
    if (!booking.getUserId().equals(userId)) {
      return ResponseEntity.status(403).build();
    }
    
    return ResponseEntity.ok(new BookingDetails(
        booking.getId(),
        booking.getUserId(),
        booking.getRoomId(),
        booking.getStatus().name(),
        booking.getStartDate(),
        booking.getEndDate()));
  }

  @DeleteMapping("/booking/{id}")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ResponseEntity<BookingResult> cancel(
      @PathVariable Long id,
      @RequestHeader("Authorization") String bearer,
      Principal principal) {
    Long userId = users.findByUsername(principal.getName())
        .map(u -> u.getId())
        .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
    
    Booking booking = bookings.findById(id)
        .orElseThrow(() -> new RuntimeException("BOOKING_NOT_FOUND"));
    
    if (!booking.getUserId().equals(userId)) {
      return ResponseEntity.status(403).build();
    }
    
    if (booking.getStatus() == Booking.Status.CONFIRMED && booking.getRoomId() != null) {
      try {
        String serviceBearer = "Bearer " + tokenProvider.issue("booking-api", "ROLE_ADMIN", INTERNAL_TOKEN_TTL_SECONDS);
        accommodationClient.release(serviceBearer, booking.getRoomId(), booking.getRequestId());
      } catch (Exception ignored) {
      }
    }
    
    booking.setStatus(Booking.Status.CANCELLED);
    bookings.save(booking);
    
    return ResponseEntity.ok(new BookingResult(
        booking.getId(),
        booking.getStatus().name(),
        booking.getRoomId()));
  }
}

package ru.mephi.bookingapi.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(
    name = "bookings",
    indexes = @Index(name = "ux_booking_request", columnList = "request_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "room_id")
  private Long roomId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  private LocalDate startDate;
  private LocalDate endDate;

  @Column(name = "request_id", nullable = false, unique = true)
  private String requestId;

  public enum Status {
    PENDING,
    CONFIRMED,
    CANCELLED
  }
}

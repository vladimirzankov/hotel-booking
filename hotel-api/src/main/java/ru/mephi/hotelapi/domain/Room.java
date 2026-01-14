package ru.mephi.hotelapi.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long hotelId;

  private String number;

  @Column(nullable = false)
  private boolean available = true;

  @Column(nullable = false)
  private int timesBooked = 0;
}

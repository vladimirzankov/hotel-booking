package ru.mephi.hotelapi.repo;

import ru.mephi.hotelapi.domain.RoomReservation;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {
  Optional<RoomReservation> findByRequestId(String requestId);

  @Query(
      "select h from RoomReservation h where h.roomId = :roomId and h.status <> 'RELEASED' and (:start <= h.endDate and :end >= h.startDate)")
  List<RoomReservation> findIntersecting(Long roomId, LocalDate start, LocalDate end);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select h from RoomReservation h where h.roomId = :roomId and h.status <> 'RELEASED' and (:start <= h.endDate and :end >= h.startDate)")
  List<RoomReservation> findIntersectingForUpdate(Long roomId, LocalDate start, LocalDate end);

  @Modifying
  @Query(
      "update RoomReservation h set h.status = :status where h.requestId = :requestId and h.roomId = :roomId")
  int updateStatusByRequest(String requestId, Long roomId, RoomReservation.Status status);
}

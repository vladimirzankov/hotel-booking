package ru.mephi.hotelapi.repo;

import ru.mephi.hotelapi.domain.RoomReservation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RoomReservationRepositoryTest {

    @Autowired
    private RoomReservationRepository holds;

    @Autowired
    private EntityManager entityManager;

    private static final Long ROOM_ID = 1L;

    @BeforeEach
    void setUp() {
        holds.deleteAll();
    }

    @Test
    void findByRequestId_returnsHold_whenExists() {
        String requestId = UUID.randomUUID().toString();
        RoomReservation saved = holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(requestId)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        Optional<RoomReservation> found = holds.findByRequestId(requestId);

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findByRequestId_returnsEmpty_whenNotExists() {
        Optional<RoomReservation> found = holds.findByRequestId("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void findIntersecting_returnsHold_whenDatesOverlap() {
        holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 5))
                .endDate(LocalDate.of(2025, 10, 10))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        List<RoomReservation> result = holds.findIntersecting(
                ROOM_ID,
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 10, 7)
        );

        assertEquals(1, result.size());
    }

    @Test
    void findIntersecting_returnsEmpty_whenNoOverlap() {
        holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        List<RoomReservation> result = holds.findIntersecting(
                ROOM_ID,
                LocalDate.of(2025, 10, 10),
                LocalDate.of(2025, 10, 15)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void findIntersecting_excludesReleasedHolds() {
        holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 5))
                .endDate(LocalDate.of(2025, 10, 10))
                .status(RoomReservation.Status.RELEASED)
                .build());

        List<RoomReservation> result = holds.findIntersecting(
                ROOM_ID,
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 10, 7)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void findIntersecting_includesHeldStatus() {
        holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 5))
                .endDate(LocalDate.of(2025, 10, 10))
                .status(RoomReservation.Status.HELD)
                .build());

        List<RoomReservation> result = holds.findIntersecting(
                ROOM_ID,
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 10, 7)
        );

        assertEquals(1, result.size());
    }

    @Test
    void findIntersecting_includesCommittedStatus() {
        holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 5))
                .endDate(LocalDate.of(2025, 10, 10))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        List<RoomReservation> result = holds.findIntersecting(
                ROOM_ID,
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 10, 7)
        );

        assertEquals(1, result.size());
    }

    @Test
    void findIntersecting_returnsOnlyMatchingRoom() {
        holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 5))
                .endDate(LocalDate.of(2025, 10, 10))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        holds.save(RoomReservation.builder()
                .roomId(999L)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 5))
                .endDate(LocalDate.of(2025, 10, 10))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        List<RoomReservation> result = holds.findIntersecting(
                ROOM_ID,
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 10, 7)
        );

        assertEquals(1, result.size());
        assertEquals(ROOM_ID, result.get(0).getRoomId());
    }

    @Test
    void findIntersecting_adjacentDates_noOverlap() {
        holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        List<RoomReservation> result = holds.findIntersecting(
                ROOM_ID,
                LocalDate.of(2025, 10, 6),
                LocalDate.of(2025, 10, 10)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void findIntersecting_sameDay_isOverlap() {
        holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 5))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        List<RoomReservation> result = holds.findIntersecting(
                ROOM_ID,
                LocalDate.of(2025, 10, 5),
                LocalDate.of(2025, 10, 5)
        );

        assertEquals(1, result.size());
    }

    @Test
    @Transactional
    void updateStatusByRequest_updatesStatus() {
        String requestId = UUID.randomUUID().toString();
        holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(requestId)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        int updated = holds.updateStatusByRequest(requestId, ROOM_ID, RoomReservation.Status.RELEASED);
        entityManager.flush();
        entityManager.clear();

        assertEquals(1, updated);
        
        RoomReservation found = holds.findByRequestId(requestId).orElseThrow();
        assertEquals(RoomReservation.Status.RELEASED, found.getStatus());
    }

    @Test
    void updateStatusByRequest_returnsZero_whenNotFound() {
        int updated = holds.updateStatusByRequest("nonexistent", ROOM_ID, RoomReservation.Status.RELEASED);

        assertEquals(0, updated);
    }

    @Test
    @Transactional
    void updateStatusByRequest_requiresMatchingRoomId() {
        String requestId = UUID.randomUUID().toString();
        holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(requestId)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        int updated = holds.updateStatusByRequest(requestId, 999L, RoomReservation.Status.RELEASED);
        entityManager.flush();
        entityManager.clear();

        assertEquals(0, updated);
        
        RoomReservation found = holds.findByRequestId(requestId).orElseThrow();
        assertEquals(RoomReservation.Status.COMMITTED, found.getStatus());
    }

    @Test
    void save_generatesId() {
        RoomReservation hold = holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.HELD)
                .build());

        assertNotNull(hold.getId());
    }

    @Test
    void findById_returnsHold() {
        RoomReservation saved = holds.save(RoomReservation.builder()
                .roomId(ROOM_ID)
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        Optional<RoomReservation> found = holds.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }
}

package ru.mephi.bookingapi.repo;

import ru.mephi.bookingapi.domain.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookings;

    @BeforeEach
    void setUp() {
        bookings.deleteAll();
    }

    @Test
    void findByRequestId_returnsBooking_whenExists() {
        String requestId = UUID.randomUUID().toString();
        Booking booking = bookings.save(Booking.builder()
                .userId(1L)
                .roomId(10L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .requestId(requestId)
                .build());

        Optional<Booking> found = bookings.findByRequestId(requestId);

        assertTrue(found.isPresent());
        assertEquals(booking.getId(), found.get().getId());
        assertEquals(requestId, found.get().getRequestId());
    }

    @Test
    void findByRequestId_returnsEmpty_whenNotExists() {
        Optional<Booking> found = bookings.findByRequestId("nonexistent-request-id");

        assertFalse(found.isPresent());
    }

    @Test
    void findByRequestId_returnsCorrectBooking_amongMany() {
        String targetRequestId = UUID.randomUUID().toString();
        
        bookings.save(Booking.builder()
                .userId(1L)
                .roomId(10L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        Booking target = bookings.save(Booking.builder()
                .userId(2L)
                .roomId(20L)
                .status(Booking.Status.PENDING)
                .startDate(LocalDate.of(2025, 11, 1))
                .endDate(LocalDate.of(2025, 11, 5))
                .requestId(targetRequestId)
                .build());

        bookings.save(Booking.builder()
                .userId(3L)
                .roomId(30L)
                .status(Booking.Status.CANCELLED)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        Optional<Booking> found = bookings.findByRequestId(targetRequestId);

        assertTrue(found.isPresent());
        assertEquals(target.getId(), found.get().getId());
        assertEquals(2L, found.get().getUserId());
    }

    @Test
    void findByUserId_returnsAllUserBookings() {
        Long userId = 100L;
        
        bookings.save(Booking.builder()
                .userId(userId)
                .roomId(10L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        bookings.save(Booking.builder()
                .userId(userId)
                .roomId(20L)
                .status(Booking.Status.PENDING)
                .startDate(LocalDate.of(2025, 11, 1))
                .endDate(LocalDate.of(2025, 11, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        bookings.save(Booking.builder()
                .userId(userId)
                .roomId(30L)
                .status(Booking.Status.CANCELLED)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        List<Booking> found = bookings.findByUserId(userId);

        assertEquals(3, found.size());
        assertTrue(found.stream().allMatch(b -> b.getUserId().equals(userId)));
    }

    @Test
    void findByUserId_returnsEmptyList_whenNoBookings() {
        List<Booking> found = bookings.findByUserId(999L);

        assertNotNull(found);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByUserId_returnsOnlyUserBookings_notOthers() {
        Long userId = 100L;
        Long otherUserId = 200L;

        bookings.save(Booking.builder()
                .userId(userId)
                .roomId(10L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        bookings.save(Booking.builder()
                .userId(otherUserId)
                .roomId(20L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 11, 1))
                .endDate(LocalDate.of(2025, 11, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        bookings.save(Booking.builder()
                .userId(otherUserId)
                .roomId(30L)
                .status(Booking.Status.PENDING)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        List<Booking> found = bookings.findByUserId(userId);

        assertEquals(1, found.size());
        assertEquals(userId, found.get(0).getUserId());
    }

    @Test
    void save_generatesId() {
        Booking booking = bookings.save(Booking.builder()
                .userId(1L)
                .roomId(10L)
                .status(Booking.Status.PENDING)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        assertNotNull(booking.getId());
    }

    @Test
    void findById_returnsBooking() {
        Booking saved = bookings.save(Booking.builder()
                .userId(1L)
                .roomId(10L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        Optional<Booking> found = bookings.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void delete_removesBooking() {
        Booking saved = bookings.save(Booking.builder()
                .userId(1L)
                .roomId(10L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        bookings.delete(saved);

        assertFalse(bookings.findById(saved.getId()).isPresent());
    }

    @Test
    void update_statusChange_persists() {
        Booking saved = bookings.save(Booking.builder()
                .userId(1L)
                .roomId(10L)
                .status(Booking.Status.PENDING)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        saved.setStatus(Booking.Status.CONFIRMED);
        bookings.save(saved);

        Booking found = bookings.findById(saved.getId()).orElseThrow();
        assertEquals(Booking.Status.CONFIRMED, found.getStatus());
    }
}

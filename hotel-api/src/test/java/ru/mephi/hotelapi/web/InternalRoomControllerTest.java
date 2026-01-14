package ru.mephi.hotelapi.web;

import ru.mephi.hotelapi.domain.Room;
import ru.mephi.hotelapi.domain.RoomReservation;
import ru.mephi.hotelapi.repo.RoomReservationRepository;
import ru.mephi.hotelapi.repo.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalRoomControllerTest {

    @Autowired MockMvc mvc;
    @Autowired RoomRepository rooms;
    @Autowired RoomReservationRepository holds;
    @Autowired ObjectMapper objectMapper;

    private Room testRoom;

    @BeforeEach
    void setUp() {
        holds.deleteAll();
        rooms.deleteAll();
        testRoom = rooms.save(Room.builder().hotelId(1L).number("101").available(true).build());
    }

    @Test
    void confirm_createsHold_whenRoomAvailable() throws Exception {
        String requestId = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of(
                "requestId", requestId,
                "start", "2025-10-01",
                "end", "2025-10-05"
        ));

        mvc.perform(post("/internal/rooms/{roomId}/confirm-availability", testRoom.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMMITTED"))
                .andExpect(jsonPath("$.roomId").value(testRoom.getId()));

        assertTrue(holds.findByRequestId(requestId).isPresent());
    }

    @Test
    void confirm_isIdempotent_returnsSameResult() throws Exception {
        String requestId = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of(
                "requestId", requestId,
                "start", "2025-10-01",
                "end", "2025-10-05"
        ));

        mvc.perform(post("/internal/rooms/{roomId}/confirm-availability", testRoom.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMMITTED"));

        mvc.perform(post("/internal/rooms/{roomId}/confirm-availability", testRoom.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMMITTED"));
    }

    @Test
    void confirm_fails_whenRoomAlreadyBooked() throws Exception {
        holds.save(RoomReservation.builder()
                .roomId(testRoom.getId())
                .requestId(UUID.randomUUID().toString())
                .startDate(LocalDate.of(2025, 10, 3))
                .endDate(LocalDate.of(2025, 10, 8))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        String requestId = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of(
                "requestId", requestId,
                "start", "2025-10-01",
                "end", "2025-10-05"
        ));

        mvc.perform(post("/internal/rooms/{roomId}/confirm-availability", testRoom.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_ERROR"))
                .andExpect(jsonPath("$.message").value("ROOM_NOT_AVAILABLE"));
    }

    @Test
    void confirm_forbidden_forNonAdmin() throws Exception {
        String requestId = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of(
                "requestId", requestId,
                "start", "2025-10-01",
                "end", "2025-10-05"
        ));

        mvc.perform(post("/internal/rooms/{roomId}/confirm-availability", testRoom.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirm_unauthorized_withoutToken() throws Exception {
        String requestId = UUID.randomUUID().toString();
        String body = objectMapper.writeValueAsString(Map.of(
                "requestId", requestId,
                "start", "2025-10-01",
                "end", "2025-10-05"
        ));

        mvc.perform(post("/internal/rooms/{roomId}/confirm-availability", testRoom.getId())
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void release_releasesExistingHold() throws Exception {
        String requestId = UUID.randomUUID().toString();
        holds.save(RoomReservation.builder()
                .roomId(testRoom.getId())
                .requestId(requestId)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        mvc.perform(post("/internal/rooms/{roomId}/release", testRoom.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("X-Request-Id", requestId))
                .andExpect(status().isAccepted());

        RoomReservation hold = holds.findByRequestId(requestId).orElseThrow();
        assertEquals(RoomReservation.Status.RELEASED, hold.getStatus());
    }

    @Test
    void release_succeeds_evenWhenNoHold() throws Exception {
        String requestId = UUID.randomUUID().toString();

        mvc.perform(post("/internal/rooms/{roomId}/release", testRoom.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("X-Request-Id", requestId))
                .andExpect(status().isAccepted());
    }

    @Test
    void release_isIdempotent() throws Exception {
        String requestId = UUID.randomUUID().toString();
        holds.save(RoomReservation.builder()
                .roomId(testRoom.getId())
                .requestId(requestId)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .status(RoomReservation.Status.COMMITTED)
                .build());

        mvc.perform(post("/internal/rooms/{roomId}/release", testRoom.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("X-Request-Id", requestId))
                .andExpect(status().isAccepted());

        mvc.perform(post("/internal/rooms/{roomId}/release", testRoom.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("X-Request-Id", requestId))
                .andExpect(status().isAccepted());
    }

    @Test
    void release_forbidden_forNonAdmin() throws Exception {
        mvc.perform(post("/internal/rooms/{roomId}/release", testRoom.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .header("X-Request-Id", "req-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void release_unauthorized_withoutToken() throws Exception {
        mvc.perform(post("/internal/rooms/{roomId}/release", testRoom.getId())
                        .header("X-Request-Id", "req-123"))
                .andExpect(status().isUnauthorized());
    }
}

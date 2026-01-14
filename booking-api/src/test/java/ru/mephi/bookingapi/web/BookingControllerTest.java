package ru.mephi.bookingapi.web;

import ru.mephi.bookingapi.domain.Booking;
import ru.mephi.bookingapi.domain.User;
import ru.mephi.bookingapi.repo.BookingRepository;
import ru.mephi.bookingapi.repo.UserRepository;
import ru.mephi.bookingapi.test.JwtTestUtils;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerTest {

    @Autowired MockMvc mvc;
    @Autowired BookingRepository bookings;
    @Autowired UserRepository users;

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-test-secret";
    
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("hotel.base-url", () -> "http://localhost:" + wm.getPort());
        r.add("auth.jwt.secret", () -> SECRET);
    }

    private String bearerUser(String username) {
        return "Bearer " + JwtTestUtils.issueHs256(SECRET, username, "ROLE_USER", 3600);
    }

    private User testUser;

    @BeforeEach
    void setUp() {
        bookings.deleteAll();
        users.deleteAll();
        wm.resetAll();
        
        testUser = users.save(User.builder()
                .username("testuser")
                .passwordHash("$2a$10$dummy")
                .role("ROLE_USER")
                .build());

        wm.stubFor(WireMock.post(urlPathMatching("/internal/rooms/.*/release"))
                .willReturn(aResponse().withStatus(202)));
    }

    @Test
    void listMyBookings_returnsEmptyList_whenNoBookings() throws Exception {
        mvc.perform(get("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listMyBookings_returnsUserBookings() throws Exception {
        bookings.save(Booking.builder()
                .userId(testUser.getId())
                .roomId(1L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .requestId(UUID.randomUUID().toString())
                .build());
        
        bookings.save(Booking.builder()
                .userId(testUser.getId())
                .roomId(2L)
                .status(Booking.Status.PENDING)
                .startDate(LocalDate.of(2025, 11, 1))
                .endDate(LocalDate.of(2025, 11, 3))
                .requestId(UUID.randomUUID().toString())
                .build());

        mvc.perform(get("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].userId", everyItem(equalTo(testUser.getId().intValue()))));
    }

    @Test
    void listMyBookings_doesNotReturnOtherUsersBookings() throws Exception {
        User otherUser = users.save(User.builder()
                .username("otheruser")
                .passwordHash("$2a$10$dummy")
                .role("ROLE_USER")
                .build());

        bookings.save(Booking.builder()
                .userId(otherUser.getId())
                .roomId(1L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 10, 1))
                .endDate(LocalDate.of(2025, 10, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        mvc.perform(get("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listMyBookings_unauthorized_withoutToken() throws Exception {
        mvc.perform(get("/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_returnsBooking_whenOwner() throws Exception {
        Booking booking = bookings.save(Booking.builder()
                .userId(testUser.getId())
                .roomId(5L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        mvc.perform(get("/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking.getId()))
                .andExpect(jsonPath("$.roomId").value(5))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.startDate").value("2025-12-01"))
                .andExpect(jsonPath("$.endDate").value("2025-12-05"));
    }

    @Test
    void getById_returnsForbidden_whenNotOwner() throws Exception {
        User otherUser = users.save(User.builder()
                .username("otheruser2")
                .passwordHash("$2a$10$dummy")
                .role("ROLE_USER")
                .build());

        Booking booking = bookings.save(Booking.builder()
                .userId(otherUser.getId())
                .roomId(5L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        mvc.perform(get("/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_returns500_whenBookingNotFound() throws Exception {
        mvc.perform(get("/booking/{id}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void cancel_cancelsConfirmedBooking_andReleasesRoom() throws Exception {
        Booking booking = bookings.save(Booking.builder()
                .userId(testUser.getId())
                .roomId(10L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .requestId("req-cancel-" + UUID.randomUUID())
                .build());

        mvc.perform(delete("/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        wm.verify(postRequestedFor(urlPathEqualTo("/internal/rooms/10/release")));
    }

    @Test
    void cancel_cancelsPendingBooking_withoutRelease() throws Exception {
        Booking booking = bookings.save(Booking.builder()
                .userId(testUser.getId())
                .roomId(null)
                .status(Booking.Status.PENDING)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .requestId("req-pending-" + UUID.randomUUID())
                .build());

        mvc.perform(delete("/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        wm.verify(0, postRequestedFor(urlPathMatching("/internal/rooms/.*/release")));
    }

    @Test
    void cancel_returnsForbidden_whenNotOwner() throws Exception {
        User otherUser = users.save(User.builder()
                .username("otheruser3")
                .passwordHash("$2a$10$dummy")
                .role("ROLE_USER")
                .build());

        Booking booking = bookings.save(Booking.builder()
                .userId(otherUser.getId())
                .roomId(5L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .requestId(UUID.randomUUID().toString())
                .build());

        mvc.perform(delete("/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancel_returns500_whenBookingNotFound() throws Exception {
        mvc.perform(delete("/booking/{id}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void cancel_handlesReleaseFailure_gracefully() throws Exception {
        wm.stubFor(WireMock.post(urlPathMatching("/internal/rooms/.*/release"))
                .willReturn(aResponse().withStatus(500)));

        Booking booking = bookings.save(Booking.builder()
                .userId(testUser.getId())
                .roomId(10L)
                .status(Booking.Status.CONFIRMED)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .requestId("req-fail-" + UUID.randomUUID())
                .build());

        mvc.perform(delete("/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_alreadyCancelledBooking_returnsOk() throws Exception {
        Booking booking = bookings.save(Booking.builder()
                .userId(testUser.getId())
                .roomId(10L)
                .status(Booking.Status.CANCELLED)
                .startDate(LocalDate.of(2025, 12, 1))
                .endDate(LocalDate.of(2025, 12, 5))
                .requestId("req-already-cancelled-" + UUID.randomUUID())
                .build());

        mvc.perform(delete("/booking/{id}", booking.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerUser("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}

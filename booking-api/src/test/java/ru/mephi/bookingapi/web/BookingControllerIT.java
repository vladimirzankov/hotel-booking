package ru.mephi.bookingapi.web;

import ru.mephi.bookingapi.repo.BookingRepository;
import ru.mephi.bookingapi.test.JwtTestUtils;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import com.github.tomakehurst.wiremock.client.WireMock;



@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerIT {

    @Autowired MockMvc mvc;
    @Autowired BookingRepository bookings;

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-test-secret";
    private String bearerUser() {
        return "Bearer " + JwtTestUtils.issueHs256(SECRET, "user1", "ROLE_USER", 3600);
    }
    private String bearerExpired() {
        return "Bearer " + JwtTestUtils.issueHs256(SECRET, "user1", "ROLE_USER", -60);
    }

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("hotel.base-url", () -> "http://localhost:" + wm.getPort());
        r.add("auth.jwt.secret", () -> SECRET);
    }

    @BeforeEach
    void resetStubs() {
        wm.resetAll();

        wm.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/rooms/recommend"))
                .willReturn(WireMock.okJson("[{\"id\":1}]")));

        wm.stubFor(WireMock.post(WireMock.urlPathMatching("/internal/rooms/1/confirm-availability"))
                .willReturn(WireMock.ok()));
    }

    @Test
    void booking_happy_autoselect_confirmed() throws Exception {
        String rid = UUID.randomUUID().toString();

        mvc.perform(post("/booking")
                        .header(HttpHeaders.AUTHORIZATION, bearerUser())
                        .header("X-Request-Id", rid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"hotelId":1,"autoSelect":true,"start":"2025-10-25","end":"2025-10-27"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomId").value(1));
    }

    @Test
    void booking_idempotent_same_requestId_returns_same_result() throws Exception {
        String rid = UUID.randomUUID().toString();

        mvc.perform(post("/booking")
                        .header(HttpHeaders.AUTHORIZATION, bearerUser())
                        .header("X-Request-Id", rid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"hotelId":1,"autoSelect":true,"start":"2025-10-25","end":"2025-10-27"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mvc.perform(post("/booking")
                        .header(HttpHeaders.AUTHORIZATION, bearerUser())
                        .header("X-Request-Id", rid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"hotelId":1,"autoSelect":true,"start":"2025-10-25","end":"2025-10-27"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void booking_conflict_from_hms_results_cancelled_503() throws Exception {
        String rid = UUID.randomUUID().toString();

        wm.stubFor(WireMock.post(WireMock.urlPathMatching("/internal/rooms/1/confirm-availability"))
                .willReturn(WireMock.aResponse().withStatus(409).withHeader("Content-Type","application/json")
                        .withBody("{\"code\":\"ROOM_NOT_AVAILABLE\"}")));

        mvc.perform(post("/booking")
                        .header(HttpHeaders.AUTHORIZATION, bearerUser())
                        .header("X-Request-Id", rid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"hotelId":1,"autoSelect":true,"start":"2025-10-25","end":"2025-10-27"}
                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void booking_timeout_from_hms_results_cancelled_504() throws Exception {
        String rid = UUID.randomUUID().toString();

        wm.stubFor(WireMock.post(WireMock.urlPathMatching("/internal/rooms/1/confirm-availability"))
                .willReturn(WireMock.aResponse().withFixedDelay(5000).withStatus(200)));

        mvc.perform(post("/booking")
                        .header(HttpHeaders.AUTHORIZATION, bearerUser())
                        .header("X-Request-Id", rid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"hotelId":1,"autoSelect":true,"start":"2025-10-25","end":"2025-10-27"}
                """))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void expired_jwt_unauthorized() throws Exception {
        String rid = UUID.randomUUID().toString();
        mvc.perform(get("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearerExpired())
                        .header("X-Request-Id", rid))
                .andExpect(status().isUnauthorized());
    }
}

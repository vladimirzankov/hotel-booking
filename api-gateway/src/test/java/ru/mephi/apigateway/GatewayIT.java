package ru.mephi.apigateway;

import ru.mephi.apigateway.test.JwtTestUtils;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;


import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayIT {

    @Autowired
    WebTestClient web;

    private static final String SECRET = "this-is-a-test-secret-32+bytes-long-OK";

    private String bearerUser() {
        return "Bearer " + JwtTestUtils.issueHs256(SECRET, "u1", "ROLE_USER", 3600);
    }

    @RegisterExtension
    static WireMockExtension hotel = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();
    @RegisterExtension
    static WireMockExtension booking = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry r) {
        r.add("spring.cloud.gateway.routes[0].id", () -> "hotel");
        r.add("spring.cloud.gateway.routes[0].uri", () -> "http://localhost:" + hotel.getPort());
        r.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/api/hotels/**,/api/rooms/**");

        r.add("spring.cloud.gateway.routes[1].id", () -> "booking");
        r.add("spring.cloud.gateway.routes[1].uri", () -> "http://localhost:" + booking.getPort());
        r.add("spring.cloud.gateway.routes[1].predicates[0]", () -> "Path=/user/**,/booking/**,/bookings/**");

        r.add("spring.cloud.gateway.routes[2].id", () -> "deny-internal");
        r.add("spring.cloud.gateway.routes[2].uri", () -> "no://op");
        r.add("spring.cloud.gateway.routes[2].predicates[0]", () -> "Path=/internal/**");
        r.add("spring.cloud.gateway.routes[2].filters[0]", () -> "SetStatus=404");

        r.add("auth.jwt.secret", () -> SECRET);
    }

    @BeforeEach
    void reset() {
        hotel.resetAll();
        booking.resetAll();
    }

    @Test
    void g4_jwt_required_for_protected_routes() {
        web.get().uri("/api/hotels").exchange().expectStatus().isUnauthorized();
    }
}

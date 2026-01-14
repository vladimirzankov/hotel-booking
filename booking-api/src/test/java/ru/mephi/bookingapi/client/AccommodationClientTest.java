package ru.mephi.bookingapi.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class AccommodationClientTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private AccommodationClient client;

    @BeforeEach
    void setUp() {
        RemoteClientConfig props = new RemoteClientConfig(1000, 2000, 1, 100);
        client = new AccommodationClient(props, "http://localhost:" + wm.getPort());
        wm.resetAll();
    }

    @Test
    void confirm_sendsCorrectRequest() {
        wm.stubFor(post(urlPathEqualTo("/internal/rooms/1/confirm-availability"))
                .willReturn(ok()));

        assertDoesNotThrow(() ->
                client.confirm("Bearer token123", 1L, "req-123", "2025-10-01", "2025-10-05")
        );

        wm.verify(postRequestedFor(urlPathEqualTo("/internal/rooms/1/confirm-availability"))
                .withHeader("Authorization", equalTo("Bearer token123"))
                .withHeader("X-Request-Id", equalTo("req-123")));
    }

    @Test
    void confirm_sendsRequestBody() {
        wm.stubFor(post(urlPathEqualTo("/internal/rooms/2/confirm-availability"))
                .willReturn(ok()));

        client.confirm("Bearer xyz", 2L, "req-456", "2025-11-01", "2025-11-10");

        wm.verify(postRequestedFor(urlPathEqualTo("/internal/rooms/2/confirm-availability"))
                .withRequestBody(matchingJsonPath("$.start", equalTo("2025-11-01")))
                .withRequestBody(matchingJsonPath("$.end", equalTo("2025-11-10")))
                .withRequestBody(matchingJsonPath("$.requestId", equalTo("req-456"))));
    }

    @Test
    void confirm_throwsOnServerError() {
        wm.stubFor(post(urlPathEqualTo("/internal/rooms/1/confirm-availability"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(Exception.class, () ->
                client.confirm("Bearer token", 1L, "req-789", "2025-10-01", "2025-10-05")
        );
    }

    @Test
    void confirm_throwsOn409Conflict() {
        wm.stubFor(post(urlPathEqualTo("/internal/rooms/1/confirm-availability"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"ROOM_NOT_AVAILABLE\"}")));

        assertThrows(Exception.class, () ->
                client.confirm("Bearer token", 1L, "req-conflict", "2025-10-01", "2025-10-05")
        );
    }

    @Test
    void confirm_retriesOnFailure() {
        wm.stubFor(post(urlPathEqualTo("/internal/rooms/1/confirm-availability"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("first-fail"));

        wm.stubFor(post(urlPathEqualTo("/internal/rooms/1/confirm-availability"))
                .inScenario("retry")
                .whenScenarioStateIs("first-fail")
                .willReturn(ok()));

        assertDoesNotThrow(() ->
                client.confirm("Bearer token", 1L, "req-retry", "2025-10-01", "2025-10-05")
        );
    }

    @Test
    void recommend_returnsListOfRooms() {
        wm.stubFor(get(urlPathEqualTo("/api/rooms/recommend"))
                .willReturn(okJson("[{\"id\":1,\"number\":\"101\"},{\"id\":2,\"number\":\"102\"}]")));

        List<?> result = client.recommend("Bearer token", 1L, "2025-10-01", "2025-10-05", 5);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void recommend_sendsCorrectQueryParams() {
        wm.stubFor(get(urlPathEqualTo("/api/rooms/recommend"))
                .willReturn(okJson("[]")));

        client.recommend("Bearer token", 10L, "2025-12-01", "2025-12-10", 3);

        wm.verify(getRequestedFor(urlPathEqualTo("/api/rooms/recommend"))
                .withQueryParam("hotelId", equalTo("10"))
                .withQueryParam("start", equalTo("2025-12-01"))
                .withQueryParam("end", equalTo("2025-12-10"))
                .withQueryParam("limit", equalTo("3")));
    }

    @Test
    void recommend_sendsAuthorizationHeader() {
        wm.stubFor(get(urlPathEqualTo("/api/rooms/recommend"))
                .willReturn(okJson("[]")));

        client.recommend("Bearer mytoken", 1L, "2025-10-01", "2025-10-05", 5);

        wm.verify(getRequestedFor(urlPathEqualTo("/api/rooms/recommend"))
                .withHeader("Authorization", equalTo("Bearer mytoken")));
    }

    @Test
    void recommend_returnsEmptyListWhenNoRooms() {
        wm.stubFor(get(urlPathEqualTo("/api/rooms/recommend"))
                .willReturn(okJson("[]")));

        List<?> result = client.recommend("Bearer token", 1L, "2025-10-01", "2025-10-05", 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void recommend_throwsOnServerError() {
        wm.stubFor(get(urlPathEqualTo("/api/rooms/recommend"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(Exception.class, () ->
                client.recommend("Bearer token", 1L, "2025-10-01", "2025-10-05", 5)
        );
    }

    @Test
    void release_sendsCorrectRequest() {
        wm.stubFor(post(urlPathEqualTo("/internal/rooms/5/release"))
                .willReturn(ok()));

        assertDoesNotThrow(() ->
                client.release("Bearer token", 5L, "req-release")
        );

        wm.verify(postRequestedFor(urlPathEqualTo("/internal/rooms/5/release"))
                .withHeader("Authorization", equalTo("Bearer token"))
                .withHeader("X-Request-Id", equalTo("req-release")));
    }

    @Test
    void release_throwsOnServerError() {
        wm.stubFor(post(urlPathEqualTo("/internal/rooms/1/release"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(Exception.class, () ->
                client.release("Bearer token", 1L, "req-fail")
        );
    }

    @Test
    void release_handlesAcceptedStatus() {
        wm.stubFor(post(urlPathEqualTo("/internal/rooms/1/release"))
                .willReturn(aResponse().withStatus(202)));

        assertDoesNotThrow(() ->
                client.release("Bearer token", 1L, "req-accepted")
        );
    }
}

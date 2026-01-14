package ru.mephi.hotelapi.web;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ru.mephi.hotelapi.repo.HotelRepository;
import ru.mephi.hotelapi.repo.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired HotelRepository hotels;
    @Autowired RoomRepository rooms;

    private Long hotelId;

    @BeforeEach
    void setUp() throws Exception {
        rooms.deleteAll();
        hotels.deleteAll();

        String hotelResponse = mvc.perform(post("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"RecHotel\",\"city\":\"Berlin\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        hotelId = com.jayway.jsonpath.JsonPath.parse(hotelResponse).read("$.id", Long.class);

        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":" + hotelId + ",\"number\":\"101\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":" + hotelId + ",\"number\":\"102\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void recommend_returns_free_rooms_sorted_and_limited() throws Exception {
        mvc.perform(get("/api/rooms/recommend")
                        .queryParam("hotelId", hotelId.toString())
                        .queryParam("start", "2025-10-25")
                        .queryParam("end", "2025-10-27")
                        .queryParam("limit", "1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", notNullValue()));
    }
}

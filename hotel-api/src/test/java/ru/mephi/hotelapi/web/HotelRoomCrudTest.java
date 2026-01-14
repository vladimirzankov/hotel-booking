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
class HotelRoomCrudTest {

    @Autowired MockMvc mvc;
    @Autowired HotelRepository hotels;
    @Autowired RoomRepository rooms;

    @BeforeEach
    void setUp() {
        rooms.deleteAll();
        hotels.deleteAll();
    }

    @Test
    void admin_can_create_hotel_and_room_user_can_read_lists() throws Exception {
        String hotelResponse = mvc.perform(post("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TestHotel\",\"city\":\"Berlin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long hotelId = com.jayway.jsonpath.JsonPath.parse(hotelResponse).read("$.id", Long.class);

        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":" + hotelId + ",\"number\":\"101\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.available").value(true));

        mvc.perform(get("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));

        mvc.perform(get("/api/rooms").queryParam("hotelId", hotelId.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hotelId").value(hotelId.intValue()));
    }

}

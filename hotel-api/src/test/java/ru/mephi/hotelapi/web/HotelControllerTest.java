package ru.mephi.hotelapi.web;

import ru.mephi.hotelapi.repo.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotelControllerTest {

    @Autowired MockMvc mvc;
    @Autowired HotelRepository hotels;

    @BeforeEach
    void setUp() {
        hotels.deleteAll();
    }

    @Test
    void createHotel_adminCanCreate() throws Exception {
        mvc.perform(post("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Grand Hotel\",\"city\":\"Paris\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Grand Hotel"))
                .andExpect(jsonPath("$.city").value("Paris"));
    }

    @Test
    void createHotel_userCannotCreate() throws Exception {
        mvc.perform(post("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Forbidden Hotel\",\"city\":\"Berlin\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createHotel_unauthorized_withoutToken() throws Exception {
        mvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Auth Hotel\",\"city\":\"Rome\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createHotel_setsLocationHeader() throws Exception {
        mvc.perform(post("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Location Hotel\",\"city\":\"London\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/api/hotels/")));
    }

    @Test
    void createHotel_withNullCity() throws Exception {
        mvc.perform(post("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No City Hotel\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("No City Hotel"))
                .andExpect(jsonPath("$.city").isEmpty());
    }

    @Test
    void listHotels_userCanList() throws Exception {
        mvc.perform(post("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hotel One\",\"city\":\"City One\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void listHotels_adminCanList() throws Exception {
        mvc.perform(get("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void listHotels_unauthorized_withoutToken() throws Exception {
        mvc.perform(get("/api/hotels"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listHotels_returnsEmptyList_whenNoHotels() throws Exception {
        mvc.perform(get("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listHotels_returnsMultipleHotels() throws Exception {
        mvc.perform(post("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hotel A\",\"city\":\"City A\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hotel B\",\"city\":\"City B\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/hotels")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}

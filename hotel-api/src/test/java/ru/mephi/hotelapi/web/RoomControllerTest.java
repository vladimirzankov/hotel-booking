package ru.mephi.hotelapi.web;

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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoomControllerTest {

    @Autowired MockMvc mvc;
    @Autowired RoomRepository rooms;

    @BeforeEach
    void setUp() {
        rooms.deleteAll();
    }

    @Test
    void createRoom_adminCanCreate() throws Exception {
        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":1,\"number\":\"101\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.number").value("101"));
    }

    @Test
    void createRoom_userCannotCreate() throws Exception {
        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":1,\"number\":\"102\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRoom_unauthorized_withoutToken() throws Exception {
        mvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":1,\"number\":\"103\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRoom_setsLocationHeader() throws Exception {
        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":2,\"number\":\"401\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/api/rooms/")));
    }

    @Test
    void createRoom_defaultsAvailableToTrue() throws Exception {
        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":3,\"number\":\"201\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void createRoom_canSetAvailableToFalse() throws Exception {
        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":4,\"number\":\"302\",\"available\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void listRooms_userCanList() throws Exception {
        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":5,\"number\":\"501\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void listRooms_adminCanList() throws Exception {
        mvc.perform(get("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void listRooms_unauthorized_withoutToken() throws Exception {
        mvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listRooms_filtersByHotelId() throws Exception {
        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":6,\"number\":\"601\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":7,\"number\":\"701\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/rooms")
                        .param("hotelId", "6")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].hotelId").value(6));
    }

    @Test
    void listRooms_returnsAllRooms_whenNoHotelIdFilter() throws Exception {
        mvc.perform(post("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hotelId\":8,\"number\":\"801\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/rooms")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void listRooms_returnsEmptyList_whenNoRoomsForHotel() throws Exception {
        mvc.perform(get("/api/rooms")
                        .param("hotelId", "99999")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}

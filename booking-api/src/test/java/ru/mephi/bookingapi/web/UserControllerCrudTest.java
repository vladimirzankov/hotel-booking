package ru.mephi.bookingapi.web;

import ru.mephi.bookingapi.domain.User;
import ru.mephi.bookingapi.repo.UserRepository;
import ru.mephi.bookingapi.test.JwtTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerCrudTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-test-secret";

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("auth.jwt.secret", () -> SECRET);
    }

    private String bearerAdmin() {
        return "Bearer " + JwtTestUtils.issueHs256(SECRET, "admin", "ROLE_ADMIN", 3600);
    }

    private String bearerUser() {
        return "Bearer " + JwtTestUtils.issueHs256(SECRET, "user1", "ROLE_USER", 3600);
    }

    @BeforeEach
    void setUp() {
        users.deleteAll();
    }

    @Test
    void register_createsUserWithUserRole() throws Exception {
        mvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"pass123\",\"admin\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));

        assertTrue(users.findByUsername("newuser").isPresent());
        assertEquals("ROLE_USER", users.findByUsername("newuser").get().getRole());
    }

    @Test
    void register_createsAdminWhenAdminTrue() throws Exception {
        mvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"adminuser\",\"password\":\"pass123\",\"admin\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())));

        assertEquals("ROLE_ADMIN", users.findByUsername("adminuser").get().getRole());
    }

    @Test
    void register_failsWhenUsernameExists() throws Exception {
        users.save(User.builder()
                .username("existing")
                .passwordHash(encoder.encode("pass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"existing\",\"password\":\"newpass\",\"admin\":false}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void register_hashesPassword() throws Exception {
        mvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hashtest\",\"password\":\"mypassword\",\"admin\":false}"))
                .andExpect(status().isOk());

        User user = users.findByUsername("hashtest").orElseThrow();
        assertNotEquals("mypassword", user.getPasswordHash());
        assertTrue(encoder.matches("mypassword", user.getPasswordHash()));
    }

    @Test
    void auth_succeedsWithCorrectCredentials() throws Exception {
        users.save(User.builder()
                .username("authuser")
                .passwordHash(encoder.encode("correctpass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"authuser\",\"password\":\"correctpass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    void auth_failsWithWrongPassword() throws Exception {
        users.save(User.builder()
                .username("authuser2")
                .passwordHash(encoder.encode("correctpass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"authuser2\",\"password\":\"wrongpass\"}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void auth_failsWhenUserNotFound() throws Exception {
        mvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nonexistent\",\"password\":\"anypass\"}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void createUser_adminCanCreateUser() throws Exception {
        mvc.perform(post("/user")
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"createduser\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("createduser"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void createUser_adminCanCreateWithSpecificRole() throws Exception {
        mvc.perform(post("/user")
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newadmin\",\"password\":\"pass123\",\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void createUser_userCannotCreate() throws Exception {
        mvc.perform(post("/user")
                        .header(HttpHeaders.AUTHORIZATION, bearerUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"attempt\",\"password\":\"pass123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_failsWhenUsernameExists() throws Exception {
        users.save(User.builder()
                .username("exists")
                .passwordHash(encoder.encode("pass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(post("/user")
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"exists\",\"password\":\"newpass\"}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void updateUser_adminCanUpdateUsername() throws Exception {
        User user = users.save(User.builder()
                .username("oldname")
                .passwordHash(encoder.encode("pass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(patch("/user/{id}", user.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newname\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newname"));

        assertFalse(users.findByUsername("oldname").isPresent());
        assertTrue(users.findByUsername("newname").isPresent());
    }

    @Test
    void updateUser_adminCanUpdatePassword() throws Exception {
        User user = users.save(User.builder()
                .username("passupdate")
                .passwordHash(encoder.encode("oldpass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(patch("/user/{id}", user.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"newpass\"}"))
                .andExpect(status().isOk());

        User updated = users.findById(user.getId()).orElseThrow();
        assertTrue(encoder.matches("newpass", updated.getPasswordHash()));
    }

    @Test
    void updateUser_adminCanUpdateRole() throws Exception {
        User user = users.save(User.builder()
                .username("roleupdate")
                .passwordHash(encoder.encode("pass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(patch("/user/{id}", user.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void updateUser_failsWhenUserNotFound() throws Exception {
        mvc.perform(patch("/user/{id}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newname\"}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void updateUser_failsWhenNewUsernameTaken() throws Exception {
        users.save(User.builder()
                .username("taken")
                .passwordHash(encoder.encode("pass"))
                .role("ROLE_USER")
                .build());

        User user = users.save(User.builder()
                .username("original")
                .passwordHash(encoder.encode("pass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(patch("/user/{id}", user.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"taken\"}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void updateUser_userCannotUpdate() throws Exception {
        User user = users.save(User.builder()
                .username("target")
                .passwordHash(encoder.encode("pass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(patch("/user/{id}", user.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hacked\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_adminCanDelete() throws Exception {
        User user = users.save(User.builder()
                .username("todelete")
                .passwordHash(encoder.encode("pass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(delete("/user/{id}", user.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
                .andExpect(status().isNoContent());

        assertFalse(users.existsById(user.getId()));
    }

    @Test
    void deleteUser_failsWhenUserNotFound() throws Exception {
        mvc.perform(delete("/user/{id}", 99999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void deleteUser_userCannotDelete() throws Exception {
        User user = users.save(User.builder()
                .username("protected")
                .passwordHash(encoder.encode("pass"))
                .role("ROLE_USER")
                .build());

        mvc.perform(delete("/user/{id}", user.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerUser()))
                .andExpect(status().isForbidden());

        assertTrue(users.existsById(user.getId()));
    }
}

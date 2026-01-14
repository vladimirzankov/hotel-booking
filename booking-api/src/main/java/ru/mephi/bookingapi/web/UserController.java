package ru.mephi.bookingapi.web;

import ru.mephi.bookingapi.config.TokenProvider;
import ru.mephi.bookingapi.domain.User;
import ru.mephi.bookingapi.repo.UserRepository;
import ru.mephi.bookingapi.web.dto.*;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
  private static final long TTL = Duration.ofHours(1).toSeconds();
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final TokenProvider tokenProvider;

  @PostMapping("/register")
  public ResponseEntity<TokenResponse> register(@RequestBody RegistrationRequest req) {
    users
        .findByUsername(req.username())
        .ifPresent(
            u -> {
              throw new RuntimeException("USERNAME_TAKEN");
            });
    String role = req.admin() ? "ROLE_ADMIN" : "ROLE_USER";
    User saved =
        users.save(
            User.builder()
                .username(req.username())
                .passwordHash(encoder.encode(req.password()))
                .role(role)
                .build());
    String token = tokenProvider.issue(saved.getUsername(), saved.getRole(), TTL);
    return ResponseEntity.ok(new TokenResponse(token, TTL));
  }

  @PostMapping("/auth")
  public ResponseEntity<TokenResponse> auth(@RequestBody LoginRequest req) {
    var u =
        users.findByUsername(req.username()).orElseThrow(() -> new RuntimeException("NOT_FOUND"));
    if (!encoder.matches(req.password(), u.getPasswordHash()))
      throw new RuntimeException("BAD_CREDENTIALS");
    String token = tokenProvider.issue(u.getUsername(), u.getRole(), TTL);
    return ResponseEntity.ok(new TokenResponse(token, TTL));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserInfo> createUser(@RequestBody NewUserRequest req) {
    users
        .findByUsername(req.username())
        .ifPresent(
            u -> {
              throw new RuntimeException("USERNAME_TAKEN");
            });
    String role = req.role() != null ? req.role() : "ROLE_USER";
    User saved =
        users.save(
            User.builder()
                .username(req.username())
                .passwordHash(encoder.encode(req.password()))
                .role(role)
                .build());
    return ResponseEntity.ok(new UserInfo(saved.getId(), saved.getUsername(), saved.getRole()));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserInfo> updateUser(
      @PathVariable Long id,
      @RequestBody ModifyUserRequest req) {
    User user = users.findById(id)
        .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
    
    if (req.username() != null && !req.username().isBlank()) {
      users.findByUsername(req.username())
          .filter(u -> !u.getId().equals(id))
          .ifPresent(u -> {
            throw new RuntimeException("USERNAME_TAKEN");
          });
      user.setUsername(req.username());
    }
    if (req.password() != null && !req.password().isBlank()) {
      user.setPasswordHash(encoder.encode(req.password()));
    }
    if (req.role() != null && !req.role().isBlank()) {
      user.setRole(req.role());
    }
    
    User saved = users.save(user);
    return ResponseEntity.ok(new UserInfo(saved.getId(), saved.getUsername(), saved.getRole()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    if (!users.existsById(id)) {
      throw new RuntimeException("USER_NOT_FOUND");
    }
    users.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}

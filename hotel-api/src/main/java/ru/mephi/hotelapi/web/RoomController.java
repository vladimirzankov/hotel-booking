package ru.mephi.hotelapi.web;

import ru.mephi.hotelapi.domain.Room;
import ru.mephi.hotelapi.repo.RoomRepository;
import ru.mephi.hotelapi.web.dto.NewRoomRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
  private final RoomRepository rooms;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Room> create(@Valid @RequestBody NewRoomRequest req) {
    Room r =
        rooms.save(
            Room.builder()
                .hotelId(req.hotelId())
                .number(req.number())
                .available(req.available() == null ? true : req.available())
                .build());
    return ResponseEntity.created(URI.create("/api/rooms/" + r.getId())).body(r);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<Room> list(@RequestParam(required = false) Long hotelId) {
    return hotelId == null ? rooms.findAll() : rooms.findByHotelId(hotelId);
  }
}

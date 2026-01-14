package ru.mephi.hotelapi.repo;

import ru.mephi.hotelapi.domain.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {}

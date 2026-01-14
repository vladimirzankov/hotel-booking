package ru.mephi.commonlib.error.exceptions;

import ru.mephi.commonlib.error.BusinessException;
import org.springframework.http.HttpStatus;

public class RoomNotAvailableException extends BusinessException {
  public RoomNotAvailableException() {
    super("ROOM_NOT_AVAILABLE", "Room is not available for these dates", HttpStatus.CONFLICT);
  }
}

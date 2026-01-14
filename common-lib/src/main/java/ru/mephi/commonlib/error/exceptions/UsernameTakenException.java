package ru.mephi.commonlib.error.exceptions;

import ru.mephi.commonlib.error.BusinessException;
import org.springframework.http.HttpStatus;

public class UsernameTakenException extends BusinessException {
  public UsernameTakenException() {
    super("USERNAME_TAKEN", "Username already exists", HttpStatus.CONFLICT);
  }
}

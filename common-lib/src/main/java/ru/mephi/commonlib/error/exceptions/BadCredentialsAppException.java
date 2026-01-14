package ru.mephi.commonlib.error.exceptions;

import ru.mephi.commonlib.error.BusinessException;
import org.springframework.http.HttpStatus;

public class BadCredentialsAppException extends BusinessException {
  public BadCredentialsAppException() {
    super("BAD_CREDENTIALS", "Bad credentials", HttpStatus.UNAUTHORIZED);
  }
}

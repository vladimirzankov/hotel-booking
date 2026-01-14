package ru.mephi.commonlib.error.exceptions;

import ru.mephi.commonlib.error.BusinessException;
import org.springframework.http.HttpStatus;

public class ServiceErrorException extends BusinessException {
  public ServiceErrorException(String msg) {
    super("SERVICE_ERROR", msg, HttpStatus.SERVICE_UNAVAILABLE);
  }
}

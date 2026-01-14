package ru.mephi.commonlib.error;

import java.time.Instant;
import java.util.Map;

public record ErrorEnvelope(
    String code, String message, String requestId, Instant timestamp, Map<String, Object> details) {
  public static ErrorEnvelope of(
      String code, String message, String requestId, Map<String, Object> details) {
    return new ErrorEnvelope(
        code, message, requestId, Instant.now(), details == null ? Map.of() : details);
  }
}

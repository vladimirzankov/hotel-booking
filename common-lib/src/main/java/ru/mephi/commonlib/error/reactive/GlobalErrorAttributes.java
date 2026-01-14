package ru.mephi.commonlib.error.reactive;

import ru.mephi.commonlib.error.BusinessException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;

public class GlobalErrorAttributes extends DefaultErrorAttributes {

  @Override
  public Map<String, Object> getErrorAttributes(
      ServerRequest request, ErrorAttributeOptions options) {
    Throwable ex = getError(request);
    String rid = request.headers().firstHeader("X-Request-Id");

    String code = "SERVICE_ERROR";
    String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Service error";
    HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;

    if (ex instanceof BusinessException be) {
      code = be.code();
      status = be.status();
      msg = be.getMessage();
    } else if (ex instanceof org.springframework.security.access.AccessDeniedException) {
      code = "FORBIDDEN";
      status = HttpStatus.FORBIDDEN;
      msg = "Forbidden";
    } else if (ex instanceof org.springframework.security.core.AuthenticationException) {
      code = "UNAUTHORIZED";
      status = HttpStatus.UNAUTHORIZED;
      msg = "Unauthorized";
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("code", code);
    out.put("message", msg != null ? msg : "Service error");
    if (rid != null) out.put("requestId", rid);
    out.put("timestamp", java.time.Instant.now().toString());
    out.put("status", status.value());
    return out;
  }
}

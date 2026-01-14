package ru.mephi.commonlib.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MdcFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String BOOKING_ID_HEADER = "X-Booking-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getName() != null) {
        MDC.put("userId", authentication.getName());
      }

      String requestId = request.getHeader(REQUEST_ID_HEADER);
      if (requestId != null && requestId.isBlank() == false) {
        MDC.put("requestId", requestId);
      }

      String bookingId = request.getHeader(BOOKING_ID_HEADER);
      if (bookingId != null && bookingId.isBlank() == false) {
        MDC.put("bookingId", bookingId);
      }

      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}

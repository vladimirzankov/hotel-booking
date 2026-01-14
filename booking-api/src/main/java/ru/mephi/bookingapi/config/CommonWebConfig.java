package ru.mephi.bookingapi.config;

import ru.mephi.commonlib.error.servlet.GlobalExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GlobalExceptionHandler.class)
public class CommonWebConfig {}

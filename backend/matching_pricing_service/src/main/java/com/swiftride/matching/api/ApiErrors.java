package com.swiftride.matching.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,String> invalid(Exception e) { return Map.of("message", e.getMessage()); }
}

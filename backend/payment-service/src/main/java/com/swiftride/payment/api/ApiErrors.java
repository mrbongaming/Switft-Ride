package com.swiftride.payment.api;

import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String,String> missing(Exception e) { return Map.of("message", e.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,String> invalid(Exception e) { return Map.of("message", e.getMessage()); }
}

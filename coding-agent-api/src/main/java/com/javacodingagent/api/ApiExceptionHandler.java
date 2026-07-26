package com.javacodingagent.api;
import com.javacodingagent.common.web.ApiResponse; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler { @ExceptionHandler({IllegalArgumentException.class, SecurityException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST) ApiResponse<Void> invalid(RuntimeException exception) { return ApiResponse.failure("INVALID_REQUEST", exception.getMessage()); } }

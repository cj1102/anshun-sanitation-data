package com.anshun.dms.common;

import org.springframework.http.HttpStatus;

/** Domain-level error with an HTTP status, kept out of controllers and mappers. */
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() { return status; }

    public static BusinessException notFound(String message) { return new BusinessException(HttpStatus.NOT_FOUND, message); }
    public static BusinessException conflict(String message) { return new BusinessException(HttpStatus.CONFLICT, message); }
    public static BusinessException badRequest(String message) { return new BusinessException(HttpStatus.BAD_REQUEST, message); }
    public static BusinessException unauthorized(String message) { return new BusinessException(HttpStatus.UNAUTHORIZED, message); }
    public static BusinessException unavailable(String message) { return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, message); }
    public static BusinessException tooManyRequests(String message) { return new BusinessException(HttpStatus.TOO_MANY_REQUESTS, message); }
}

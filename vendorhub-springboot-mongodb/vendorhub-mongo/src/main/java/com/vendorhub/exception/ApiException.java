package com.vendorhub.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(String msg, HttpStatus status) { super(msg); this.status = status; }
    public HttpStatus getStatus() { return status; }

    public static ApiException notFound(String m)    { return new ApiException(m, HttpStatus.NOT_FOUND); }
    public static ApiException forbidden(String m)   { return new ApiException(m, HttpStatus.FORBIDDEN); }
    public static ApiException badRequest(String m)  { return new ApiException(m, HttpStatus.BAD_REQUEST); }
    public static ApiException conflict(String m)    { return new ApiException(m, HttpStatus.CONFLICT); }
    public static ApiException unauthorized(String m){ return new ApiException(m, HttpStatus.UNAUTHORIZED); }
}

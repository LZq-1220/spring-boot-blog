package com.blog.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常基类。带上 HTTP 状态码，由 GlobalExceptionHandler 统一转成 {"message": "..."}。
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

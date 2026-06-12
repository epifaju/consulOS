package com.consulos.common;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String errorCode,
        LocalDateTime timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, message, errorCode, LocalDateTime.now());
    }
}

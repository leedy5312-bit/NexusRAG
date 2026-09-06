package com.rag.nexusrag.common.response;

import com.rag.nexusrag.common.enums.ErrorCode;

public record ApiResponse<T>(
        Integer code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null);
    }

    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(500, message, null);
    }
}

package com.anshun.dms.common;

/** Standard API envelope for newly refactored business modules. */
public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> success(T data) { return new ApiResponse<>(0, "success", data); }
    public static ApiResponse<Void> successMessage(String message) { return new ApiResponse<>(0, message, null); }
    public static ApiResponse<Void> failure(int code, String message) { return new ApiResponse<>(code, message, null); }
}

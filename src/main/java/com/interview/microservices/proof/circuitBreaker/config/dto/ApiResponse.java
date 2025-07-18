package com.interview.microservices.proof.circuitBreaker.config.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private String source; // "LIVE", "CACHE", "FALLBACK"

    public static <T> ApiResponse<T> success(T data, String source) {
        return new ApiResponse<>(true, data, "Success", source);
    }

    public static <T> ApiResponse<T> fallback(T data, String message) {
        return new ApiResponse<>(false, data, message, "FALLBACK");
    }
}

package com.example.demo.Dto;

import java.time.Instant;

public record ErrorResponse(
    String timestamp,
    int status,
    String error,
    String message,
    String path
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now().toString(), status, error, message, path);
    }
}


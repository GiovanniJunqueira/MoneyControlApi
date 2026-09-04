package com.financeiro.api.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(String error, LocalDateTime timestamp, List<FieldError> details) {

    public ErrorResponse(String error) {
        this(error, LocalDateTime.now(), null);
    }

    public record FieldError(String field, String message) {
    }
}

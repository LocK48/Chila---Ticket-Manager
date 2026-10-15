package com.chila.backend.common.api;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        int status,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}

package com.ghiriish.eventplatform.producer.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record EventRequest(
    @NotBlank String eventType,
    @NotBlank String userId,
    String sessionId,
    @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
    Map<String, Object> metadata
) {
}

package com.chatapp.api.dto;

import jakarta.validation.constraints.NotNull;

public record MuteRequest(
        @NotNull MuteDuration duration
) {
    public enum MuteDuration {
        HOURS_8, WEEK_1, INDEFINITE
    }
}

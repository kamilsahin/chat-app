package com.chatapp.internal.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddMembersRequest(
        @NotEmpty List<String> memberIds
) {}

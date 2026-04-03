package com.merdeleine.gatewaybff.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BatchUserLookupRequest(
        @NotEmpty
        @Size(max = 200)
        List<@NotNull UUID> customerIds
) {}


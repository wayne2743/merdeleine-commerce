package com.merdeleine.catalog.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OpenPaymentRequest {
    private Integer overrideTtlMinutes; // optional

    public Integer getOverrideTtlMinutes() { return overrideTtlMinutes; }
    public void setOverrideTtlMinutes(Integer overrideTtlMinutes) { this.overrideTtlMinutes = overrideTtlMinutes; }
}


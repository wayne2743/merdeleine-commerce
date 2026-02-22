package com.merdeleine.production.planning.dto;

public class OpenPaymentRequest {
    // optional：測試或臨時 override
    private Integer overrideTtlMinutes;

    public Integer getOverrideTtlMinutes() { return overrideTtlMinutes; }
    public void setOverrideTtlMinutes(Integer overrideTtlMinutes) { this.overrideTtlMinutes = overrideTtlMinutes; }
}
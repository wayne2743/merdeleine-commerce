package com.merdeleine.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record ProductNextGroupOpenAtResponse(
        @JsonProperty("next_group_open_at") OffsetDateTime nextGroupOpenAt,
        @JsonProperty("default_min_qty") Integer defaultMinQty,
        @JsonProperty("default_max_qty") Integer defaultMaxQty,
        @JsonProperty("default_leads_day") Integer defaultLeadsDay,
        @JsonProperty("default_ship_day") Integer defaultShipDay,
        @JsonProperty("default_open_days") Integer defaultOpenDays
) {}


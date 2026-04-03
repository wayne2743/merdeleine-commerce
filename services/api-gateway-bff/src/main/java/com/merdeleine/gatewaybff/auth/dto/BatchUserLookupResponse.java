package com.merdeleine.gatewaybff.auth.dto;

import java.util.List;

public record BatchUserLookupResponse(
        List<UserLookupResponse> users,
        List<String> missingCustomerIds
) {}


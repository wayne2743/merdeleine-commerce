package com.merdeleine.notification.controller;

import com.merdeleine.notification.dto.LineAccountLinkStartRequest;
import com.merdeleine.notification.dto.LineAccountLinkStartResponse;
import com.merdeleine.notification.exception.BadRequestException;
import com.merdeleine.notification.service.LineAccountLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/members/me/line")
@Tag(name = "Member LINE Link", description = "Member-facing LINE account linking")
public class MemberLineLinkController {

    private final LineAccountLinkService lineAccountLinkService;

    public MemberLineLinkController(LineAccountLinkService lineAccountLinkService) {
        this.lineAccountLinkService = lineAccountLinkService;
    }

    @PostMapping("/link/start")
    @Operation(summary = "Start LINE account link for the logged-in member")
    public ResponseEntity<LineAccountLinkStartResponse> startLink(
            @RequestHeader(value = "X-USER-ID", required = false) String memberIdHeader,
            @Valid @RequestBody LineAccountLinkStartRequest request) {
        UUID memberId = resolveMemberId(memberIdHeader);
        return ResponseEntity.ok(lineAccountLinkService.startLink(memberId, request.getLineUserId()));
    }

    @DeleteMapping("/link")
    @Operation(summary = "Unlink the LINE account from the logged-in member")
    public ResponseEntity<Void> unlink(
            @RequestHeader(value = "X-USER-ID", required = false) String memberIdHeader) {
        UUID memberId = resolveMemberId(memberIdHeader);
        lineAccountLinkService.unlink(memberId);
        return ResponseEntity.noContent().build();
    }

    /**
     * The authenticated memberId is forwarded by the API gateway via X-USER-ID;
     * it must never come from the request body.
     */
    private UUID resolveMemberId(String memberIdHeader) {
        if (!StringUtils.hasText(memberIdHeader)) {
            throw new BadRequestException("Missing authenticated member identity");
        }
        try {
            return UUID.fromString(memberIdHeader);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid member identity");
        }
    }
}

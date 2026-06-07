package com.merdeleine.notification.service;

import com.merdeleine.notification.client.LineMessagingApiClient;
import com.merdeleine.notification.config.LineAccountLinkProperties;
import com.merdeleine.notification.dto.LineAccountLinkStartResponse;
import com.merdeleine.notification.dto.line.LineWebhookEvent;
import com.merdeleine.notification.dto.line.LineWebhookLink;
import com.merdeleine.notification.dto.line.LineWebhookRequest;
import com.merdeleine.notification.dto.line.LineWebhookSource;
import com.merdeleine.notification.entity.LineAccountLinkNonce;
import com.merdeleine.notification.entity.LineUser;
import com.merdeleine.notification.enums.LineAccountLinkStatus;
import com.merdeleine.notification.exception.ConflictException;
import com.merdeleine.notification.repository.LineAccountLinkNonceRepository;
import com.merdeleine.notification.repository.LineUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LineAccountLinkServiceTest {

    private LineUserRepository lineUserRepository;
    private LineAccountLinkNonceRepository nonceRepository;
    private LineMessagingApiClient lineMessagingApiClient;
    private LineAccountLinkProperties props;
    private LineAccountLinkService service;

    private static final String LINE_USER_ID = "U1234567890abcdef";

    @BeforeEach
    void setUp() {
        lineUserRepository = mock(LineUserRepository.class);
        nonceRepository = mock(LineAccountLinkNonceRepository.class);
        lineMessagingApiClient = mock(LineMessagingApiClient.class);
        props = new LineAccountLinkProperties();
        props.setAccountLinkBaseUrl("https://access.line.me/dialog/bot/accountLink");
        props.setNonceExpireMinutes(10);
        service = new LineAccountLinkService(lineUserRepository, nonceRepository, lineMessagingApiClient, props);

        when(nonceRepository.save(any(LineAccountLinkNonce.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lineUserRepository.save(any(LineUser.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void startLink_success_createsPendingNonceAndReturnsUrl() {
        UUID memberId = UUID.randomUUID();
        when(lineUserRepository.findByUserId(LINE_USER_ID)).thenReturn(Optional.empty());
        when(lineUserRepository.findByMemberId(memberId)).thenReturn(Optional.empty());
        when(lineMessagingApiClient.issueLinkToken(LINE_USER_ID)).thenReturn("link-token-123");

        LineAccountLinkStartResponse response = service.startLink(memberId, LINE_USER_ID);

        ArgumentCaptor<LineAccountLinkNonce> captor = ArgumentCaptor.forClass(LineAccountLinkNonce.class);
        verify(nonceRepository).save(captor.capture());
        LineAccountLinkNonce savedNonce = captor.getValue();

        assertThat(savedNonce.getStatus()).isEqualTo(LineAccountLinkStatus.PENDING);
        assertThat(savedNonce.getMemberId()).isEqualTo(memberId);
        assertThat(savedNonce.getLineUserId()).isEqualTo(LINE_USER_ID);
        assertThat(savedNonce.getLinkToken()).isEqualTo("link-token-123");
        assertThat(savedNonce.getExpiredAt()).isAfter(OffsetDateTime.now());

        assertThat(response.accountLinkUrl()).contains("linkToken=link-token-123");
        assertThat(response.accountLinkUrl()).contains("nonce=" + savedNonce.getNonce());
    }

    @Test
    void startLink_lineUserAlreadyBoundToAnotherMember_throwsConflict() {
        UUID memberId = UUID.randomUUID();
        UUID otherMember = UUID.randomUUID();

        LineUser existing = new LineUser();
        existing.setUserId(LINE_USER_ID);
        existing.setMemberId(otherMember);
        when(lineUserRepository.findByUserId(LINE_USER_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.startLink(memberId, LINE_USER_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("已被其他會員綁定");

        verify(lineMessagingApiClient, never()).issueLinkToken(anyString());
        verify(nonceRepository, never()).save(any());
    }

    @Test
    void handleWebhook_accountLinkOk_bindsMemberAndMarksNonceUsed() {
        UUID memberId = UUID.randomUUID();

        LineAccountLinkNonce nonce = pendingNonce(memberId, "nonce-abc", OffsetDateTime.now().plusMinutes(5));
        when(nonceRepository.findByNonce("nonce-abc")).thenReturn(Optional.of(nonce));

        LineUser lineUser = new LineUser();
        lineUser.setUserId(LINE_USER_ID);
        lineUser.setFollowed(false);
        when(lineUserRepository.findByUserId(LINE_USER_ID)).thenReturn(Optional.of(lineUser));

        service.handleAccountLinkWebhook(okEvent(LINE_USER_ID, "nonce-abc"));

        assertThat(lineUser.getMemberId()).isEqualTo(memberId);
        assertThat(lineUser.isFollowed()).isTrue();
        verify(lineUserRepository).save(lineUser);

        assertThat(nonce.getStatus()).isEqualTo(LineAccountLinkStatus.USED);
        assertThat(nonce.getUsedAt()).isNotNull();
    }

    @Test
    void handleWebhook_expiredNonce_doesNotBindAndNotUsed() {
        UUID memberId = UUID.randomUUID();

        LineAccountLinkNonce nonce = pendingNonce(memberId, "nonce-exp", OffsetDateTime.now().minusMinutes(1));
        when(nonceRepository.findByNonce("nonce-exp")).thenReturn(Optional.of(nonce));

        service.handleAccountLinkWebhook(okEvent(LINE_USER_ID, "nonce-exp"));

        assertThat(nonce.getStatus()).isNotEqualTo(LineAccountLinkStatus.USED);
        verify(lineUserRepository, never()).findByUserId(anyString());
        verify(lineUserRepository, never()).save(any());
    }

    @Test
    void handleWebhook_resendUsedNonce_isIdempotentNoError() {
        UUID memberId = UUID.randomUUID();

        LineAccountLinkNonce nonce = pendingNonce(memberId, "nonce-used", OffsetDateTime.now().plusMinutes(5));
        nonce.setStatus(LineAccountLinkStatus.USED);
        nonce.setUsedAt(OffsetDateTime.now().minusMinutes(1));
        when(nonceRepository.findByNonce("nonce-used")).thenReturn(Optional.of(nonce));

        service.handleAccountLinkWebhook(okEvent(LINE_USER_ID, "nonce-used"));

        verify(lineUserRepository, never()).save(any());
        verify(nonceRepository, never()).save(any());
        assertThat(nonce.getStatus()).isEqualTo(LineAccountLinkStatus.USED);
    }

    @Test
    void unlink_clearsMemberId() {
        UUID memberId = UUID.randomUUID();
        LineUser lineUser = new LineUser();
        lineUser.setUserId(LINE_USER_ID);
        lineUser.setMemberId(memberId);
        when(lineUserRepository.findByMemberId(memberId)).thenReturn(Optional.of(lineUser));

        service.unlink(memberId);

        assertThat(lineUser.getMemberId()).isNull();
        verify(lineUserRepository).save(lineUser);
    }

    // ---- helpers ----

    private LineAccountLinkNonce pendingNonce(UUID memberId, String nonceValue, OffsetDateTime expiredAt) {
        LineAccountLinkNonce nonce = new LineAccountLinkNonce();
        nonce.setId(UUID.randomUUID());
        nonce.setNonce(nonceValue);
        nonce.setMemberId(memberId);
        nonce.setLineUserId(LINE_USER_ID);
        nonce.setLinkToken("link-token");
        nonce.setStatus(LineAccountLinkStatus.PENDING);
        nonce.setExpiredAt(expiredAt);
        return nonce;
    }

    private LineWebhookRequest okEvent(String lineUserId, String nonce) {
        LineWebhookEvent event = new LineWebhookEvent(
                "accountLink",
                new LineWebhookSource("user", lineUserId),
                new LineWebhookLink("ok", nonce)
        );
        return new LineWebhookRequest(List.of(event));
    }
}

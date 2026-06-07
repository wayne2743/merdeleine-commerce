package com.merdeleine.notification.service;

import com.merdeleine.notification.client.LineMessagingApiClient;
import com.merdeleine.notification.config.LineAccountLinkProperties;
import com.merdeleine.notification.dto.LineAccountLinkStartResponse;
import com.merdeleine.notification.dto.line.LineWebhookEvent;
import com.merdeleine.notification.dto.line.LineWebhookRequest;
import com.merdeleine.notification.entity.LineAccountLinkNonce;
import com.merdeleine.notification.entity.LineUser;
import com.merdeleine.notification.enums.LineAccountLinkStatus;
import com.merdeleine.notification.exception.BadRequestException;
import com.merdeleine.notification.exception.ConflictException;
import com.merdeleine.notification.repository.LineAccountLinkNonceRepository;
import com.merdeleine.notification.repository.LineUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class LineAccountLinkService {

    private static final Logger log = LoggerFactory.getLogger(LineAccountLinkService.class);
    private static final String ACCOUNT_LINK_EVENT = "accountLink";
    private static final String LINK_RESULT_OK = "ok";
    private static final int NONCE_BYTES = 16; // 128 bits

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

    private final LineUserRepository lineUserRepository;
    private final LineAccountLinkNonceRepository nonceRepository;
    private final LineMessagingApiClient lineMessagingApiClient;
    private final LineAccountLinkProperties props;

    public LineAccountLinkService(LineUserRepository lineUserRepository,
                                  LineAccountLinkNonceRepository nonceRepository,
                                  LineMessagingApiClient lineMessagingApiClient,
                                  LineAccountLinkProperties props) {
        this.lineUserRepository = lineUserRepository;
        this.nonceRepository = nonceRepository;
        this.lineMessagingApiClient = lineMessagingApiClient;
        this.props = props;
    }

    /**
     * Starts the LINE account-link flow for a logged-in member.
     */
    @Transactional
    public LineAccountLinkStartResponse startLink(UUID memberId, String lineUserId) {
        if (memberId == null) {
            throw new BadRequestException("memberId is required");
        }
        if (!StringUtils.hasText(lineUserId)) {
            throw new BadRequestException("lineUserId is required");
        }

        // 1) Ensure a line_user row exists for this LINE userId.
        LineUser lineUser = lineUserRepository.findByUserId(lineUserId).orElse(null);
        if (lineUser == null) {
            lineUser = new LineUser();
            lineUser.setUserId(lineUserId);
            lineUser.setMemberId(null);
            lineUser.setFollowed(true);
            lineUser = lineUserRepository.save(lineUser);
        } else if (lineUser.getMemberId() != null && !lineUser.getMemberId().equals(memberId)) {
            throw new ConflictException("此 LINE 帳號已被其他會員綁定");
        }

        // 2) Ensure this member isn't already bound to a different LINE account.
        Optional<LineUser> boundToMember = lineUserRepository.findByMemberId(memberId);
        if (boundToMember.isPresent() && !boundToMember.get().getUserId().equals(lineUserId)) {
            throw new ConflictException("此會員已綁定其他 LINE 帳號");
        }

        // 3) Ask LINE for a link token.
        String linkToken = lineMessagingApiClient.issueLinkToken(lineUserId);

        // 4) Generate an unpredictable nonce and persist it as PENDING.
        String nonce = generateNonce();
        LineAccountLinkNonce entity = new LineAccountLinkNonce();
        entity.setNonce(nonce);
        entity.setMemberId(memberId);
        entity.setLineUserId(lineUserId);
        entity.setLinkToken(linkToken);
        entity.setStatus(LineAccountLinkStatus.PENDING);
        entity.setExpiredAt(OffsetDateTime.now().plusMinutes(props.getNonceExpireMinutes()));
        nonceRepository.save(entity);

        String accountLinkUrl = props.getAccountLinkBaseUrl()
                + "?linkToken=" + linkToken
                + "&nonce=" + nonce;
        return new LineAccountLinkStartResponse(accountLinkUrl);
    }

    /**
     * Handles incoming LINE webhook events, completing account-link bindings.
     */
    @Transactional
    public void handleAccountLinkWebhook(LineWebhookRequest request) {
        if (request == null || request.events() == null) {
            return;
        }
        for (LineWebhookEvent event : request.events()) {
            try {
                handleAccountLinkEvent(event);
            } catch (Exception e) {
                log.error("[LINE accountLink] error handling event", e);
            }
        }
    }

    private void handleAccountLinkEvent(LineWebhookEvent event) {
        if (event == null || !ACCOUNT_LINK_EVENT.equals(event.type())) {
            return;
        }
        if (event.link() == null || event.source() == null) {
            log.warn("[LINE accountLink] missing link/source, skip");
            return;
        }
        if (!LINK_RESULT_OK.equals(event.link().result())) {
            log.warn("[LINE accountLink] result is not ok (result={}), skip binding", event.link().result());
            return;
        }

        String lineUserId = event.source().userId();
        String nonceValue = event.link().nonce();
        if (!StringUtils.hasText(lineUserId) || !StringUtils.hasText(nonceValue)) {
            log.warn("[LINE accountLink] missing userId/nonce, skip");
            return;
        }

        LineAccountLinkNonce nonce = nonceRepository.findByNonce(nonceValue).orElse(null);
        if (nonce == null) {
            log.warn("[LINE accountLink] nonce not found, skip");
            return;
        }

        // Idempotency: webhook can be re-delivered.
        if (nonce.getStatus() == LineAccountLinkStatus.USED) {
            log.info("[LINE accountLink] nonce already USED, skip (idempotent)");
            return;
        }
        if (nonce.getStatus() == LineAccountLinkStatus.EXPIRED) {
            log.warn("[LINE accountLink] nonce already EXPIRED, skip");
            return;
        }

        if (!nonce.getLineUserId().equals(lineUserId)) {
            log.warn("[LINE accountLink] nonce lineUserId mismatch, skip");
            return;
        }

        if (nonce.getExpiredAt().isBefore(OffsetDateTime.now())) {
            log.warn("[LINE accountLink] nonce expired at {}, marking EXPIRED", nonce.getExpiredAt());
            nonce.setStatus(LineAccountLinkStatus.EXPIRED);
            nonceRepository.save(nonce);
            return;
        }

        // Bind: write member_id directly onto the existing line_user row.
        LineUser lineUser = lineUserRepository.findByUserId(lineUserId).orElseGet(() -> {
            LineUser created = new LineUser();
            created.setUserId(lineUserId);
            created.setFollowed(true);
            return created;
        });
        lineUser.setMemberId(nonce.getMemberId());
        lineUser.setFollowed(true);
        lineUserRepository.save(lineUser);

        nonce.setStatus(LineAccountLinkStatus.USED);
        nonce.setUsedAt(OffsetDateTime.now());
        nonceRepository.save(nonce);

        log.info("[LINE accountLink] bound memberId={} to lineUserId={}", nonce.getMemberId(), lineUserId);
    }

    /**
     * Unbinds the LINE account from the given member.
     */
    @Transactional
    public void unlink(UUID memberId) {
        if (memberId == null) {
            throw new BadRequestException("memberId is required");
        }
        lineUserRepository.findByMemberId(memberId).ifPresent(lineUser -> {
            lineUser.setMemberId(null);
            lineUserRepository.save(lineUser);
            log.info("[LINE accountLink] unlinked memberId={} from lineUserId={}", memberId, lineUser.getUserId());
        });
    }

    private String generateNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        secureRandom.nextBytes(bytes);
        return urlEncoder.encodeToString(bytes);
    }
}

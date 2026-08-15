package com.merdeleine.payment.service;

import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.client.NewebPayClient;
import com.merdeleine.payment.dto.NewebPayRefundResponse;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.entity.PaymentRefund;
import com.merdeleine.payment.entity.PaymentTxn;
import com.merdeleine.payment.enums.PaymentRefundStatus;
import com.merdeleine.payment.enums.PaymentTxnAction;
import com.merdeleine.payment.enums.PaymentTxnResult;
import com.merdeleine.payment.repository.PaymentRefundRepository;
import com.merdeleine.payment.repository.PaymentRepository;
import com.merdeleine.payment.repository.PaymentTxnRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class NewebPayRefundService {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentTxnRepository transactionRepository;
    private final NewebPayClient newebPayClient;

    public NewebPayRefundService(PaymentRepository paymentRepository,
                                 PaymentRefundRepository refundRepository,
                                 PaymentTxnRepository transactionRepository,
                                 NewebPayClient newebPayClient) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.transactionRepository = transactionRepository;
        this.newebPayClient = newebPayClient;
    }

    @Transactional
    public NewebPayRefundResponse refund(UUID paymentId, int amountCents, String idempotencyKey) {
        String normalizedIdempotencyKey = validateIdempotencyKey(idempotencyKey);
        newebPayClient.requireConfigured();

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: " + paymentId));

        PaymentRefund existing = refundRepository
                .findByPaymentIdAndIdempotencyKey(paymentId, normalizedIdempotencyKey)
                .orElse(null);
        if (existing != null) {
            if (!existing.getAmountCents().equals(amountCents)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency-Key was already used with a different refund amount"
                );
            }
            return toResponse(existing);
        }

        validatePayment(payment, amountCents);
        if (refundRepository.existsByPaymentIdAndStatus(paymentId, PaymentRefundStatus.UNKNOWN)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment has an UNKNOWN refund that must be reconciled before another refund"
            );
        }
        long refundedAmountCents = refundRepository.sumSucceededAmountCents(paymentId);
        if (refundedAmountCents + amountCents > payment.getAmountCents()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Refund amount exceeds refundable balance");
        }

        PaymentRefund refund = new PaymentRefund();
        refund.setId(UUID.randomUUID());
        refund.setPayment(payment);
        refund.setIdempotencyKey(normalizedIdempotencyKey);
        refund.setAmountCents(amountCents);
        refund.setStatus(PaymentRefundStatus.PENDING);
        refund.setProviderTradeNo(payment.getProviderCaptureId());
        refundRepository.saveAndFlush(refund);

        try {
            NewebPayClient.CloseResult closeResult = newebPayClient.refund(
                    payment.getProviderPaymentId(),
                    payment.getProviderCaptureId(),
                    NewebPayService.toTwdAmount(amountCents, payment.getCurrency())
            );
            refund.setProviderCode(closeResult.status());
            refund.setProviderMessage(closeResult.message());
            refund.setRawResponse(closeResult.rawResponse());

            if (closeResult.success()) {
                refund.setStatus(PaymentRefundStatus.SUCCEEDED);
                long totalRefunded = refundedAmountCents + amountCents;
                payment.setStatus(totalRefunded == payment.getAmountCents()
                        ? PaymentStatus.REFUNDED
                        : PaymentStatus.PARTIALLY_REFUNDED);
                paymentRepository.save(payment);
                saveTransaction(payment, PaymentTxnResult.OK, closeResult.rawResponse());
            } else {
                refund.setStatus(PaymentRefundStatus.FAILED);
                saveTransaction(payment, PaymentTxnResult.NG, closeResult.rawResponse());
            }
        } catch (RuntimeException ex) {
            // 藍新可能已收到請求但回應在途中遺失；保留 UNKNOWN 供 Query/人工對帳，禁止自動重送。
            refund.setStatus(PaymentRefundStatus.UNKNOWN);
            refund.setProviderCode("TRANSPORT_OR_VALIDATION_ERROR");
            refund.setProviderMessage(safeMessage(ex));
        }

        return toResponse(refundRepository.save(refund));
    }

    private void validatePayment(Payment payment, int amountCents) {
        if (payment.getProvider() != PaymentProvider.NewebPay) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment provider is not NewebPay");
        }
        if (payment.getStatus() != PaymentStatus.SUCCEEDED
                && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment cannot be refunded in status " + payment.getStatus()
            );
        }
        if (payment.getProviderPaymentId() == null || payment.getProviderPaymentId().isBlank()
                || payment.getProviderCaptureId() == null || payment.getProviderCaptureId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is missing NewebPay transaction identifiers");
        }
        NewebPayService.toTwdAmount(amountCents, payment.getCurrency());
    }

    private void saveTransaction(Payment payment, PaymentTxnResult result, String rawResponse) {
        PaymentTxn transaction = new PaymentTxn();
        transaction.setId(UUID.randomUUID());
        transaction.setPayment(payment);
        transaction.setAction(PaymentTxnAction.REFUND);
        transaction.setResult(result);
        transaction.setRawResponse(rawResponse);
        transactionRepository.save(transaction);
    }

    private static String validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must not exceed 100 characters");
        }
        return normalized;
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null) return ex.getClass().getSimpleName();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private static NewebPayRefundResponse toResponse(PaymentRefund refund) {
        return new NewebPayRefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getAmountCents(),
                refund.getStatus(),
                refund.getProviderCode(),
                refund.getProviderMessage(),
                refund.getCreatedAt(),
                refund.getUpdatedAt()
        );
    }
}

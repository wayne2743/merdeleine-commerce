package com.merdeleine.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.dto.BankTransferUpdateRequest;
import com.merdeleine.payment.dto.PaymentCreateRequest;
import com.merdeleine.payment.dto.PaymentResponse;
import com.merdeleine.payment.dto.PaymentUpdateRequest;
import com.merdeleine.payment.dto.PaymentWithSellWindowResponse;
import com.merdeleine.payment.entity.OutboxEvent;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.enums.OutboxEventStatus;
import com.merdeleine.payment.mapper.PaymentMapper;
import com.merdeleine.payment.repository.OutboxEventRepository;
import com.merdeleine.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentCrudService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final OrderQueryService orderQueryService;
    private final CatalogQueryService catalogQueryService;
    private final UserQueryService userQueryService;
    private final String paymentCompleteTopic;

    public PaymentCrudService(PaymentRepository paymentRepository,
                              OutboxEventRepository outboxEventRepository,
                              ObjectMapper objectMapper,
                              OrderQueryService orderQueryService,
                              CatalogQueryService catalogQueryService,
                              UserQueryService userQueryService,
                              @Value("${app.kafka.topic.payment-completed-events}") String paymentCompleteTopic) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.orderQueryService = orderQueryService;
        this.catalogQueryService = catalogQueryService;
        this.userQueryService = userQueryService;
        this.paymentCompleteTopic = paymentCompleteTopic;
    }

    @Transactional
    public PaymentResponse create(PaymentCreateRequest request) {
        Payment payment = new Payment();
        applyCreateRequest(payment, request);
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID paymentId) {
        return toResponse(getEntity(paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> notFoundByOrderId(orderId));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> list(UUID orderId, PaymentStatus status, Pageable pageable) {
        return queryPage(orderId, status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentWithSellWindowResponse> listWithSellWindow(UUID orderId, PaymentStatus status, Pageable pageable) {
        Page<Payment> payments = queryPage(orderId, status, pageable);

        List<UUID> orderIds = payments.getContent().stream()
                .map(Payment::getOrderId)
                .distinct()
                .toList();

        // ① 一次 batch 同時拿 sellWindowId + customerId
        OrderQueryService.OrderBatchResult orderRefs = safeGetOrderRefs(orderIds);
        Map<UUID, UUID> orderSellWindowMap = orderRefs.orderSellWindowMap();
        Map<UUID, UUID> orderCustomerMap = orderRefs.orderCustomerMap();

        // ② batch 查 sellWindowName
        List<UUID> sellWindowIds = orderSellWindowMap.values().stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<UUID, String> sellWindowNameMap = safeGetSellWindowNamesByIds(sellWindowIds);

        // ③ batch 查 customer info
        List<UUID> customerIds = orderCustomerMap.values().stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<UUID, UserQueryService.CustomerInfo> customerInfoMap = safeGetCustomerInfosByIds(customerIds);

        return payments.map(payment -> {
            UUID swId = orderSellWindowMap.get(payment.getOrderId());
            String swName = swId == null ? null : sellWindowNameMap.get(swId);
            UUID cId = orderCustomerMap.get(payment.getOrderId());
            UserQueryService.CustomerInfo customer = cId == null ? null : customerInfoMap.get(cId);
            return toWithSellWindowResponse(payment, swId, swName, cId, customer);
        });
    }

    private OrderQueryService.OrderBatchResult safeGetOrderRefs(List<UUID> orderIds) {
        try {
            return orderQueryService.batchGetOrderRefs(orderIds);
        } catch (Exception ex) {
            return new OrderQueryService.OrderBatchResult(new HashMap<>(), new HashMap<>());
        }
    }

    private Map<UUID, UUID> safeGetSellWindowIdsByOrderIds(List<UUID> orderIds) {
        try {
            return orderQueryService.getSellWindowIdsByOrderIds(orderIds);
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    private Map<UUID, String> safeGetSellWindowNamesByIds(List<UUID> sellWindowIds) {
        try {
            return catalogQueryService.getSellWindowNamesByIds(sellWindowIds);
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    private Map<UUID, UserQueryService.CustomerInfo> safeGetCustomerInfosByIds(List<UUID> customerIds) {
        try {
            return userQueryService.getCustomerInfosByIds(customerIds);
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    @Transactional
    public PaymentResponse update(UUID paymentId, PaymentUpdateRequest request) {
        Payment payment = getEntity(paymentId);
        applyUpdateRequest(payment, request);
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse approveBankTransfer(UUID paymentId) {
        Payment payment = getEntity(paymentId);

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            // 冪等：已經是 SUCCEEDED 就直接回傳，不重複發事件
            return toResponse(payment);
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);

        writeOutbox(
                "PAYMENT",
                payment.getId(),
                paymentCompleteTopic,
                PaymentMapper.toPaymentEvent(payment, paymentCompleteTopic)
        );

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse updateBankTransferByOrderId(UUID orderId, BankTransferUpdateRequest request) {
        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> notFoundByOrderId(orderId));

        payment.setBankLastFive(request.bankLastFive());
        payment.setTransferAt(request.transferAt());
        payment.setProvider(PaymentProvider.BankTransfer);

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(UUID paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw notFound(paymentId);
        }
        paymentRepository.deleteById(paymentId);
    }

    private Payment getEntity(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> notFound(paymentId));
    }

    private Page<Payment> queryPage(UUID orderId, PaymentStatus status, Pageable pageable) {
        if (orderId != null && status != null) {
            return paymentRepository.findByOrderIdAndStatus(orderId, status, pageable);
        }
        if (orderId != null) {
            return paymentRepository.findByOrderId(orderId, pageable);
        }
        if (status != null) {
            return paymentRepository.findByStatus(status, pageable);
        }
        return paymentRepository.findAll(pageable);
    }

    private void applyCreateRequest(Payment payment, PaymentCreateRequest request) {
        requireActiveProvider(request.getProvider());
        payment.setOrderId(request.getOrderId());
        payment.setProvider(request.getProvider());
        payment.setStatus(request.getStatus());
        payment.setAmountCents(request.getAmountCents());
        payment.setCurrency(request.getCurrency());
        payment.setProviderPaymentId(request.getProviderPaymentId());
        payment.setProviderCaptureId(request.getProviderCaptureId());
        payment.setApproveUrl(request.getApproveUrl());
        payment.setBankLastFive(request.getBankLastFive());
        payment.setTransferAt(request.getTransferAt());
        payment.setExpireAt(request.getExpireAt());
        payment.setExpiredAt(request.getExpiredAt());
    }

    private void applyUpdateRequest(Payment payment, PaymentUpdateRequest request) {
        requireActiveProvider(request.getProvider());
        payment.setOrderId(request.getOrderId());
        payment.setProvider(request.getProvider());
        payment.setStatus(request.getStatus());
        payment.setAmountCents(request.getAmountCents());
        payment.setCurrency(request.getCurrency());
        payment.setProviderPaymentId(request.getProviderPaymentId());
        payment.setProviderCaptureId(request.getProviderCaptureId());
        payment.setApproveUrl(request.getApproveUrl());
        payment.setBankLastFive(request.getBankLastFive());
        payment.setTransferAt(request.getTransferAt());
        payment.setExpireAt(request.getExpireAt());
        payment.setExpiredAt(request.getExpiredAt());
    }

    private void requireActiveProvider(PaymentProvider provider) {
        if (provider != PaymentProvider.BankTransfer && provider != PaymentProvider.NewebPay) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Payment provider is historical and no longer supported: " + provider
            );
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getProviderPaymentId(),
                payment.getProviderCaptureId(),
                payment.getApproveUrl(),
                payment.getBankLastFive(),
                payment.getTransferAt(),
                payment.getExpireAt(),
                payment.getExpiredAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private PaymentWithSellWindowResponse toWithSellWindowResponse(Payment payment,
                                                                   UUID sellWindowId,
                                                                   String sellWindowName,
                                                                   UUID customerId,
                                                                   UserQueryService.CustomerInfo customer) {
        return new PaymentWithSellWindowResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getProviderPaymentId(),
                payment.getProviderCaptureId(),
                payment.getApproveUrl(),
                payment.getBankLastFive(),
                payment.getTransferAt(),
                payment.getExpireAt(),
                payment.getExpiredAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                sellWindowId,
                sellWindowName,
                customerId,
                customer == null ? null : customer.name(),
                customer == null ? null : customer.phone(),
                customer == null ? null : customer.email()
        );
    }

    private ResponseStatusException notFound(UUID paymentId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: " + paymentId);
    }

    private ResponseStatusException notFoundByOrderId(UUID orderId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found by orderId: " + orderId);
    }

    private void writeOutbox(String aggregateType, UUID aggregateId, String eventType, Object payloadObj) {
        try {
            OutboxEvent evt = new OutboxEvent();
            evt.setId(UUID.randomUUID());
            evt.setAggregateType(aggregateType);
            evt.setAggregateId(aggregateId);
            evt.setEventType(eventType);
            evt.setPayload(objectMapper.valueToTree(payloadObj));
            evt.setStatus(OutboxEventStatus.NEW);
            outboxEventRepository.save(evt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write outbox event", e);
        }
    }
}

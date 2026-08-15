package com.merdeleine.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.dto.BankTransferUpdateRequest;
import com.merdeleine.payment.dto.PaymentCreateRequest;
import com.merdeleine.payment.dto.PaymentResponse;
import com.merdeleine.payment.dto.PaymentUpdateRequest;
import com.merdeleine.payment.dto.PaymentWithSellWindowResponse;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.repository.OutboxEventRepository;
import com.merdeleine.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCrudServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderQueryService orderQueryService;

    @Mock
    private CatalogQueryService catalogQueryService;

    @Mock
    private UserQueryService userQueryService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private PaymentCrudService paymentCrudService;

    @BeforeEach
    void setUp() {
        paymentCrudService = new PaymentCrudService(
                paymentRepository,
                outboxEventRepository,
                objectMapper,
                orderQueryService,
                catalogQueryService,
                userQueryService,
                "payment.completed.v1"
        );
    }

    @Test
    void createShouldPersistAndReturnResponse() {
        UUID paymentId = UUID.randomUUID();
        PaymentCreateRequest request = createRequest();

        Payment saved = new Payment();
        saved.setId(paymentId);
        saved.setOrderId(request.getOrderId());
        saved.setProvider(request.getProvider());
        saved.setStatus(PaymentStatus.INIT);
        saved.setAmountCents(request.getAmountCents());
        saved.setCurrency(request.getCurrency());
        saved.setProviderPaymentId(request.getProviderPaymentId());
        saved.setProviderCaptureId(request.getProviderCaptureId());
        saved.setApproveUrl(request.getApproveUrl());
        saved.setBankLastFive(request.getBankLastFive());
        saved.setTransferAt(request.getTransferAt());

        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponse response = paymentCrudService.create(request);

        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.orderId()).isEqualTo(request.getOrderId());
        assertThat(response.provider()).isEqualTo(PaymentProvider.NewebPay);
        assertThat(response.bankLastFive()).isEqualTo("12345");
        assertThat(response.transferAt()).isEqualTo(request.getTransferAt());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @SuppressWarnings("deprecation")
    void createShouldRejectHistoricalProvider() {
        PaymentCreateRequest request = createRequest();
        request.setProvider(PaymentProvider.PayPal);

        assertThatThrownBy(() -> paymentCrudService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("historical and no longer supported");
    }

    @Test
    void getByIdShouldReturnPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = createPayment(paymentId, PaymentStatus.PENDING);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentCrudService.getById(paymentId);

        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void getByOrderIdShouldReturnLatestPayment() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Payment payment = createPayment(paymentId, PaymentStatus.PENDING);
        payment.setOrderId(orderId);

        when(paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentCrudService.getByOrderId(orderId);

        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.orderId()).isEqualTo(orderId);
    }

    @Test
    void getByOrderIdShouldThrowWhenNotFound() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentCrudService.getByOrderId(orderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Payment not found by orderId");
    }

    @Test
    void listByOrderIdAndStatusShouldUseSpecificRepositoryMethod() {
        UUID orderId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Payment payment = createPayment(UUID.randomUUID(), PaymentStatus.PENDING);
        payment.setOrderId(orderId);

        when(paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(payment), pageable, 1));

        Page<PaymentResponse> result = paymentCrudService.list(orderId, PaymentStatus.PENDING, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(paymentRepository).findByOrderIdAndStatus(orderId, PaymentStatus.PENDING, pageable);
    }

    @Test
    void updateShouldModifyAndSaveEntity() {
        UUID paymentId = UUID.randomUUID();
        Payment existing = createPayment(paymentId, PaymentStatus.INIT);
        PaymentUpdateRequest request = createUpdateRequest();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(existing)).thenReturn(existing);

        PaymentResponse response = paymentCrudService.update(paymentId, request);

        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(existing.getProviderCaptureId()).isEqualTo("CAPTURE-1");
        assertThat(existing.getBankLastFive()).isEqualTo("54321");
        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.transferAt()).isEqualTo(request.getTransferAt());
    }

    @Test
    void updateBankTransferByOrderIdShouldForceProviderAndUpdateFields() {
        UUID orderId = UUID.randomUUID();
        OffsetDateTime transferAt = OffsetDateTime.parse("2026-04-04T11:00:00+08:00");

        Payment existing = createPayment(UUID.randomUUID(), PaymentStatus.PENDING);
        existing.setOrderId(orderId);
        existing.setProvider(PaymentProvider.NewebPay);

        when(paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(existing)).thenReturn(existing);

        PaymentResponse response = paymentCrudService.updateBankTransferByOrderId(
                orderId,
                new BankTransferUpdateRequest("67890", transferAt)
        );

        assertThat(existing.getProvider()).isEqualTo(PaymentProvider.BankTransfer);
        assertThat(existing.getBankLastFive()).isEqualTo("67890");
        assertThat(existing.getTransferAt()).isEqualTo(transferAt);
        assertThat(response.provider()).isEqualTo(PaymentProvider.BankTransfer);
        verify(paymentRepository).save(existing);
    }

    @Test
    void approveBankTransferShouldSetSucceededAndWriteOutbox() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = createPayment(paymentId, PaymentStatus.PENDING);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentCrudService.approveBankTransfer(paymentId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(outboxEventRepository).save(any());
    }

    @Test
    void approveBankTransferShouldBeIdempotentWhenAlreadySucceeded() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = createPayment(paymentId, PaymentStatus.SUCCEEDED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentCrudService.approveBankTransfer(paymentId);

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        // 冪等：已成功的不應再儲存或寫 outbox
        verify(paymentRepository, org.mockito.Mockito.never()).save(any());
        verify(outboxEventRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void deleteShouldThrowWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.existsById(paymentId)).thenReturn(false);

        assertThatThrownBy(() -> paymentCrudService.delete(paymentId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void listWithSellWindowShouldReturnEnrichedPage() {
        UUID orderId = UUID.randomUUID();
        UUID sellWindowId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Payment payment = createPayment(UUID.randomUUID(), PaymentStatus.PENDING);
        payment.setOrderId(orderId);

        OrderQueryService.OrderBatchResult batchResult = new OrderQueryService.OrderBatchResult(
                java.util.Map.of(orderId, sellWindowId),
                java.util.Map.of()
        );

        when(paymentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(payment), pageable, 1));
        when(orderQueryService.batchGetOrderRefs(List.of(orderId))).thenReturn(batchResult);
        when(catalogQueryService.getSellWindowNamesByIds(List.of(sellWindowId)))
                .thenReturn(java.util.Map.of(sellWindowId, "Spring Sale Window"));

        Page<PaymentWithSellWindowResponse> result =
                paymentCrudService.listWithSellWindow(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).sellWindowId()).isEqualTo(sellWindowId);
        assertThat(result.getContent().get(0).sellWindowName()).isEqualTo("Spring Sale Window");
    }

    @Test
    void listWithSellWindowShouldReturnEnrichedPageWithCustomerInfo() {
        UUID orderId = UUID.randomUUID();
        UUID sellWindowId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Payment payment = createPayment(UUID.randomUUID(), PaymentStatus.PENDING);
        payment.setOrderId(orderId);

        OrderQueryService.OrderBatchResult batchResult = new OrderQueryService.OrderBatchResult(
                java.util.Map.of(orderId, sellWindowId),
                java.util.Map.of(orderId, customerId)
        );

        when(paymentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(payment), pageable, 1));
        when(orderQueryService.batchGetOrderRefs(List.of(orderId))).thenReturn(batchResult);
        when(catalogQueryService.getSellWindowNamesByIds(List.of(sellWindowId)))
                .thenReturn(java.util.Map.of(sellWindowId, "Spring Sale Window"));
        when(userQueryService.getCustomerInfosByIds(List.of(customerId)))
                .thenReturn(java.util.Map.of(customerId, new UserQueryService.CustomerInfo("Alice", "0912345678", "alice@example.com")));

        Page<PaymentWithSellWindowResponse> result =
                paymentCrudService.listWithSellWindow(null, null, pageable);

        PaymentWithSellWindowResponse item = result.getContent().get(0);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(item.sellWindowId()).isEqualTo(sellWindowId);
        assertThat(item.sellWindowName()).isEqualTo("Spring Sale Window");
        assertThat(item.customerId()).isEqualTo(customerId);
        assertThat(item.customerName()).isEqualTo("Alice");
        assertThat(item.customerPhone()).isEqualTo("0912345678");
        assertThat(item.customerEmail()).isEqualTo("alice@example.com");
        assertThat(item.bankLastFive()).isEqualTo("12345");
    }

    private PaymentCreateRequest createRequest() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderId(UUID.randomUUID());
        request.setProvider(PaymentProvider.NewebPay);
        request.setAmountCents(1280);
        request.setCurrency("TWD");
        request.setProviderPaymentId("ORDER-1");
        request.setProviderCaptureId("CAPTURE-1");
        request.setApproveUrl("https://example.com/approve");
        request.setBankLastFive("12345");
        request.setTransferAt(OffsetDateTime.parse("2026-04-04T09:30:00+08:00"));
        return request;
    }

    private PaymentUpdateRequest createUpdateRequest() {
        PaymentUpdateRequest request = new PaymentUpdateRequest();
        request.setOrderId(UUID.randomUUID());
        request.setProvider(PaymentProvider.NewebPay);
        request.setStatus(PaymentStatus.PAID);
        request.setAmountCents(1280);
        request.setCurrency("TWD");
        request.setProviderPaymentId("ORDER-1");
        request.setProviderCaptureId("CAPTURE-1");
        request.setApproveUrl("https://example.com/approve");
        request.setBankLastFive("54321");
        request.setTransferAt(OffsetDateTime.parse("2026-04-04T10:00:00+08:00"));
        return request;
    }

    private Payment createPayment(UUID paymentId, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrderId(UUID.randomUUID());
        payment.setProvider(PaymentProvider.NewebPay);
        payment.setStatus(status);
        payment.setAmountCents(1280);
        payment.setCurrency("TWD");
        payment.setProviderPaymentId("ORDER-1");
        payment.setProviderCaptureId("CAPTURE-1");
        payment.setApproveUrl("https://example.com/approve");
        payment.setBankLastFive("12345");
        payment.setTransferAt(OffsetDateTime.parse("2026-04-04T09:30:00+08:00"));
        return payment;
    }
}

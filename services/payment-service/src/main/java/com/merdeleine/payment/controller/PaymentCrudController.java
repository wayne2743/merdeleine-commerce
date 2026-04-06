package com.merdeleine.payment.controller;

import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.dto.BankTransferUpdateRequest;
import com.merdeleine.payment.dto.PaymentCreateRequest;
import com.merdeleine.payment.dto.PaymentResponse;
import com.merdeleine.payment.dto.PaymentUpdateRequest;
import com.merdeleine.payment.dto.PaymentWithSellWindowResponse;
import com.merdeleine.payment.service.PaymentCrudService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentCrudController {

    private final PaymentCrudService paymentCrudService;

    public PaymentCrudController(PaymentCrudService paymentCrudService) {
        this.paymentCrudService = paymentCrudService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody PaymentCreateRequest request) {
        return paymentCrudService.create(request);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getById(@PathVariable UUID paymentId) {
        return paymentCrudService.getById(paymentId);
    }

    @GetMapping("/orders/{orderId}")
    public PaymentResponse getByOrderId(@PathVariable UUID orderId) {
        return paymentCrudService.getByOrderId(orderId);
    }

    @GetMapping
    public Page<PaymentResponse> list(@RequestParam(required = false) UUID orderId,
                                      @RequestParam(required = false) PaymentStatus status,
                                      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return paymentCrudService.list(orderId, status, pageable);
    }

    @GetMapping("/enriched")
    public Page<PaymentWithSellWindowResponse> listWithSellWindow(@RequestParam(required = false) UUID orderId,
                                                                  @RequestParam(required = false) PaymentStatus status,
                                                                  @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return paymentCrudService.listWithSellWindow(orderId, status, pageable);
    }

    @PutMapping("/{paymentId}")
    public PaymentResponse update(@PathVariable UUID paymentId,
                                  @Valid @RequestBody PaymentUpdateRequest request) {
        return paymentCrudService.update(paymentId, request);
    }

    @PatchMapping("/bank-transfer/orders/{orderId}")
    public PaymentResponse updateBankTransferByOrderId(@PathVariable UUID orderId,
                                                       @Valid @RequestBody BankTransferUpdateRequest request) {
        return paymentCrudService.updateBankTransferByOrderId(orderId, request);
    }

    /**
     * 人工審核銀行轉帳：將 payment 狀態設為 SUCCEEDED，並發出 PaymentCompletedEvent。
     */
    @PatchMapping("/{paymentId}/approve")
    public PaymentResponse approveBankTransfer(@PathVariable UUID paymentId) {
        return paymentCrudService.approveBankTransfer(paymentId);
    }

    @DeleteMapping("/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID paymentId) {
        paymentCrudService.delete(paymentId);
    }
}

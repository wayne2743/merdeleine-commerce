package com.merdeleine.payment.controller;

import com.merdeleine.payment.dto.NewebPayRefundRequest;
import com.merdeleine.payment.dto.NewebPayRefundResponse;
import com.merdeleine.payment.newebpay.NewebPayProperties;
import com.merdeleine.payment.service.NewebPayRefundService;
import com.merdeleine.payment.service.NewebPayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments/newebpay")
public class NewebPayController {

    private final NewebPayService newebPayService;
    private final NewebPayRefundService refundService;
    private final NewebPayProperties properties;

    public NewebPayController(NewebPayService newebPayService,
                              NewebPayRefundService refundService,
                              NewebPayProperties properties) {
        this.newebPayService = newebPayService;
        this.refundService = refundService;
        this.properties = properties;
    }

    @GetMapping(value = "/checkout/{paymentId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkout(@PathVariable UUID paymentId,
                                           @RequestParam(required = false) String email,
                                           @RequestParam(required = false) String itemDescription) {
        return ResponseEntity.ok(newebPayService.buildCheckoutHtml(paymentId, email, itemDescription));
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> notify(@RequestBody MultiValueMap<String, String> body) {
        newebPayService.handleCallback(body.toSingleValueMap());
        return ResponseEntity.ok("1|OK");
    }

    @PostMapping(value = "/return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> paymentReturn(@RequestBody MultiValueMap<String, String> body) {
        NewebPayService.CallbackResult result = newebPayService.handleCallback(body.toSingleValueMap());
        URI redirect = UriComponentsBuilder.fromUriString(properties.frontendReturnUrl())
                .queryParam("paymentId", result.paymentId())
                .queryParam("status", result.status())
                .build(true)
                .toUri();
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(redirect).build();
    }

    @PostMapping("/{paymentId}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    public NewebPayRefundResponse refund(@PathVariable UUID paymentId,
                                         @RequestHeader("Idempotency-Key") String idempotencyKey,
                                         @Valid @RequestBody NewebPayRefundRequest request) {
        return refundService.refund(paymentId, request.amountCents(), idempotencyKey);
    }
}

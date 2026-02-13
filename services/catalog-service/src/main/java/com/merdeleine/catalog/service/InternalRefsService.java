package com.merdeleine.catalog.service;


import com.merdeleine.catalog.dto.RefsResponse;
import com.merdeleine.catalog.repository.ProductRepository;
import com.merdeleine.catalog.repository.SellWindowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InternalRefsService {

    private final ProductRepository productRepository;
    private final SellWindowRepository sellWindowRepository;

    public InternalRefsService(ProductRepository productRepository,
                               SellWindowRepository sellWindowRepository) {
        this.productRepository = productRepository;
        this.sellWindowRepository = sellWindowRepository;
    }

    @Transactional(readOnly = true)
    public RefsResponse getRefs(UUID productId, UUID sellWindowId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        var sellWindow = sellWindowRepository.findById(sellWindowId)
                .orElseThrow(() -> new NotFoundException("SellWindow not found: " + sellWindowId));

        return new RefsResponse(productId, product.getName(), sellWindowId, sellWindow.getName());
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }
}

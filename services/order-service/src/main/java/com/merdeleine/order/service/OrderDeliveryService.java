package com.merdeleine.order.service;

import com.merdeleine.order.dto.OrderDeliveryRequest;
import com.merdeleine.order.dto.OrderDeliveryResponse;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.exception.BadRequestException;
import com.merdeleine.order.exception.ResourceNotFoundException;
import com.merdeleine.order.mapper.OrderMapper;
import com.merdeleine.order.repository.OrderRepository;
import com.merdeleine.order.repository.StorePickupLocationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderDeliveryService {

    private final OrderRepository orderRepository;
    private final StorePickupLocationRepository storePickupLocationRepository;

    public OrderDeliveryService(OrderRepository orderRepository,
                                StorePickupLocationRepository storePickupLocationRepository) {
        this.orderRepository = orderRepository;
        this.storePickupLocationRepository = storePickupLocationRepository;
    }

    @Transactional
    public OrderDeliveryResponse create(UUID orderId, OrderDeliveryRequest req) {
        Order order = findOrder(orderId);
        if (order.getDelivery() != null) {
            throw new BadRequestException("Order delivery already exists for orderId=" + orderId);
        }
        OrderDeliverySupport.applyAndValidate(order, req, storePickupLocationRepository);
        orderRepository.save(order);
        return OrderMapper.toDeliveryResponse(order.getDelivery());
    }

    @Transactional
    public OrderDeliveryResponse get(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getDelivery() == null) {
            throw new ResourceNotFoundException("Order delivery not found for orderId=" + orderId);
        }
        return OrderMapper.toDeliveryResponse(order.getDelivery());
    }

    @Transactional
    public OrderDeliveryResponse update(UUID orderId, OrderDeliveryRequest req) {
        Order order = findOrder(orderId);
        if (order.getDelivery() == null) {
            throw new ResourceNotFoundException("Order delivery not found for orderId=" + orderId);
        }
        OrderDeliverySupport.applyAndValidate(order, req, storePickupLocationRepository);
        orderRepository.save(order);
        return OrderMapper.toDeliveryResponse(order.getDelivery());
    }

    @Transactional
    public void delete(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getDelivery() == null) {
            throw new ResourceNotFoundException("Order delivery not found for orderId=" + orderId);
        }
        order.clearDelivery();
        order.setShippingAddress(null);
        orderRepository.save(order);
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }
}


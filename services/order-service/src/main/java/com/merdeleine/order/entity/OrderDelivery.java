package com.merdeleine.order.entity;

import com.merdeleine.order.enums.DeliveryMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_delivery")
public class OrderDelivery {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 40)
    private DeliveryMethodType deliveryMethod;

    @Column(name = "pickup_location_name", length = 255)
    private String pickupLocationName;

    @Column(name = "pickup_location_address", columnDefinition = "TEXT")
    private String pickupLocationAddress;

    @Column(name = "pickup_time")
    private OffsetDateTime pickupTime;

    @Column(name = "convenience_store_code", length = 50)
    private String convenienceStoreCode;

    @Column(name = "convenience_store_name", length = 255)
    private String convenienceStoreName;

    @Column(name = "convenience_store_address", columnDefinition = "TEXT")
    private String convenienceStoreAddress;

    @Column(name = "home_delivery_address", columnDefinition = "TEXT")
    private String homeDeliveryAddress;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public DeliveryMethodType getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethodType deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getPickupLocationName() {
        return pickupLocationName;
    }

    public void setPickupLocationName(String pickupLocationName) {
        this.pickupLocationName = pickupLocationName;
    }

    public String getPickupLocationAddress() {
        return pickupLocationAddress;
    }

    public void setPickupLocationAddress(String pickupLocationAddress) {
        this.pickupLocationAddress = pickupLocationAddress;
    }

    public OffsetDateTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(OffsetDateTime pickupTime) {
        this.pickupTime = pickupTime;
    }

    public String getConvenienceStoreCode() {
        return convenienceStoreCode;
    }

    public void setConvenienceStoreCode(String convenienceStoreCode) {
        this.convenienceStoreCode = convenienceStoreCode;
    }

    public String getConvenienceStoreName() {
        return convenienceStoreName;
    }

    public void setConvenienceStoreName(String convenienceStoreName) {
        this.convenienceStoreName = convenienceStoreName;
    }

    public String getConvenienceStoreAddress() {
        return convenienceStoreAddress;
    }

    public void setConvenienceStoreAddress(String convenienceStoreAddress) {
        this.convenienceStoreAddress = convenienceStoreAddress;
    }

    public String getHomeDeliveryAddress() {
        return homeDeliveryAddress;
    }

    public void setHomeDeliveryAddress(String homeDeliveryAddress) {
        this.homeDeliveryAddress = homeDeliveryAddress;
    }
}


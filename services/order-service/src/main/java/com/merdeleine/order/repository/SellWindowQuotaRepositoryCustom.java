package com.merdeleine.order.repository;

import com.merdeleine.order.dto.SellWindowQuotaBatchDto;

import java.util.List;

public interface SellWindowQuotaRepositoryCustom {
    List<com.merdeleine.order.entity.SellWindowQuota> findByKeys(List<SellWindowQuotaBatchDto.Key> keys);
}
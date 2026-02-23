package com.merdeleine.order.repository;

import com.merdeleine.order.dto.SellWindowQuotaBatchDto;
import com.merdeleine.order.entity.SellWindowQuota;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SellWindowQuotaRepositoryImpl implements SellWindowQuotaRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<SellWindowQuota> findByKeys(List<SellWindowQuotaBatchDto.Key> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(SellWindowQuota.class);
        var root = cq.from(SellWindowQuota.class);

        List<Predicate> ors = new ArrayList<>();
        for (var k : keys) {
            if (k == null || k.sellWindowId() == null || k.productId() == null) continue;
            ors.add(cb.and(
                    cb.equal(root.get("sellWindowId"), k.sellWindowId()),
                    cb.equal(root.get("productId"), k.productId())
            ));
        }

        if (ors.isEmpty()) return List.of();

        cq.select(root).where(cb.or(ors.toArray(new Predicate[0])));
        return em.createQuery(cq).getResultList();
    }
}
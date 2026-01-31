package com.merdeleine.catalog.service;


import com.merdeleine.catalog.dto.SellWindowDto;
import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.exception.BadRequestException;
import com.merdeleine.catalog.exception.NotFoundException;
import com.merdeleine.catalog.repository.SellWindowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class SellWindowService {

    private final SellWindowRepository sellWindowRepository;

    public SellWindowService(SellWindowRepository sellWindowRepository) {
        this.sellWindowRepository = sellWindowRepository;
    }

    @Transactional
    public SellWindowDto.Response create(SellWindowDto.CreateRequest req) {
        validate(req.getName(), req.getStartAt(), req.getEndAt(), req.getTimezone());

        if (sellWindowRepository.existsByName(req.getName())) {
            throw new BadRequestException("SellWindow name already exists: " + req.getName());
        }

        SellWindow e = new SellWindow();
        e.setName(req.getName());
        e.setStartAt(req.getStartAt());
        e.setEndAt(req.getEndAt());
        e.setTimezone(req.getTimezone());

        return toResponse(sellWindowRepository.save(e));
    }

    @Transactional(readOnly = true)
    public SellWindowDto.Response get(UUID id) {
        return toResponse(sellWindowRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SellWindow not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<SellWindowDto.Response> list() {
        return sellWindowRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SellWindowDto.Response update(UUID id, SellWindowDto.UpdateRequest req) {
        validate(req.getName(), req.getStartAt(), req.getEndAt(), req.getTimezone());

        SellWindow e = sellWindowRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SellWindow not found: " + id));

        if (!e.getName().equals(req.getName()) && sellWindowRepository.existsByName(req.getName())) {
            throw new BadRequestException("SellWindow name already exists: " + req.getName());
        }

        e.setName(req.getName());
        e.setStartAt(req.getStartAt());
        e.setEndAt(req.getEndAt());
        e.setTimezone(req.getTimezone());

        return toResponse(sellWindowRepository.save(e));
    }

    @Transactional
    public void delete(UUID id) {
        if (!sellWindowRepository.existsById(id)) {
            throw new NotFoundException("SellWindow not found: " + id);
        }
        sellWindowRepository.deleteById(id);
    }

    private void validate(String name, OffsetDateTime startAt, OffsetDateTime endAt, String timezone) {
        if (name == null || name.isBlank()) throw new BadRequestException("name is required");
        if (startAt == null) throw new BadRequestException("startAt is required");
        if (endAt == null) throw new BadRequestException("endAt is required");
        if (!endAt.isAfter(startAt)) throw new BadRequestException("endAt must be after startAt");
        if (timezone == null || timezone.isBlank()) throw new BadRequestException("timezone is required");
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new BadRequestException("Invalid timezone: " + timezone);
        }
    }

    private SellWindowDto.Response toResponse(SellWindow e) {
        return new SellWindowDto.Response(e.getId(), e.getName(), e.getStartAt(), e.getEndAt(), e.getTimezone());
    }
}

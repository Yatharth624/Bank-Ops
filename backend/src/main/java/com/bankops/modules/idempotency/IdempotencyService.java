package com.bankops.modules.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> getRecord(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return idempotencyRepository.findByIdempotencyKey(key.trim());
    }

    @Transactional
    public IdempotencyRecord saveRecord(String key, String resourceType, String responsePayload, int statusCode) {
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(key.trim())
                .resourceType(resourceType)
                .responsePayload(responsePayload)
                .statusCode(statusCode)
                .build();
        return idempotencyRepository.save(record);
    }
}

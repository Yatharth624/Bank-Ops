package com.bankops.modules.audit;

import com.bankops.common.CorrelationFilter;
import com.bankops.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    /**
     * Records an immutable append-only audit event in an independent transaction.
     * Note: Application-level append-only controls safeguard against unauthorized users via API,
     * but do not protect against direct modification by privileged database superusers (DBAs).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent logEvent(String action, String entityType, String entityId, String details, String status) {
        String actorUsername = SecurityUtils.getCurrentUsername();
        String actorRole = SecurityUtils.getCurrentUserPrimaryRole();
        UUID actorId = SecurityUtils.getCurrentUserId();
        String correlationId = CorrelationFilter.getCorrelationId();

        AuditEvent event = AuditEvent.builder()
                .actorId(actorId)
                .actorUsername(actorUsername)
                .actorRole(actorRole)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .correlationId(correlationId)
                .status(status != null ? status : "SUCCESS")
                .build();

        log.info("AUDIT: user='{}' role='{}' action='{}' entity='{}:{}' correlation='{}'",
                actorUsername, actorRole, action, entityType, entityId, correlationId);

        return auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventDto> searchAuditEvents(
            String entityType, String action, String actorUsername,
            Instant fromDate, Instant toDate, Pageable pageable) {
        return auditEventRepository.searchAuditEvents(entityType, action, actorUsername, fromDate, toDate, pageable)
                .map(AuditEventDto::fromEntity);
    }
}

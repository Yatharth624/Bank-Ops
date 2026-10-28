package com.bankops.modules.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDto {
    private UUID id;
    private UUID actorId;
    private String actorUsername;
    private String actorRole;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private String correlationId;
    private String status;
    private Instant createdAt;

    public static AuditEventDto fromEntity(AuditEvent event) {
        return AuditEventDto.builder()
                .id(event.getId())
                .actorId(event.getActorId())
                .actorUsername(event.getActorUsername())
                .actorRole(event.getActorRole())
                .action(event.getAction())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .details(event.getDetails())
                .correlationId(event.getCorrelationId())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .build();
    }
}

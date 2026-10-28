package com.bankops.modules.exceptioncase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseTimelineEventDto {
    private String eventType; // CASE_CREATED, NOTE_ADDED, STATUS_CHANGED, CASE_ASSIGNED
    private String actorUsername;
    private String description;
    private Instant timestamp;
    private Object payload;
}

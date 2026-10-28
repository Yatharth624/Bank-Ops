package com.bankops.modules.rule;

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
public class RuleDto {
    private UUID id;
    private String ruleCode;
    private String ruleName;
    private String description;
    private String category;
    private String severity;
    private String parameters;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static RuleDto fromEntity(ValidationRule rule) {
        return RuleDto.builder()
                .id(rule.getId())
                .ruleCode(rule.getRuleCode())
                .ruleName(rule.getRuleName())
                .description(rule.getDescription())
                .category(rule.getCategory())
                .severity(rule.getSeverity())
                .parameters(rule.getParameters())
                .active(rule.isActive())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}

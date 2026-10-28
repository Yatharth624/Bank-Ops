package com.bankops.modules.exceptioncase;

import com.bankops.modules.transaction.TransactionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionCaseDto {
    private UUID id;
    private String caseNumber;
    private TransactionDto transaction;
    private UUID ruleId;
    private String ruleCode;
    private String category;
    private String severity;
    private String status;
    private UUID assignedAnalystId;
    private String assignedAnalystUsername;
    private String assignedAnalystFullName;
    private String resolutionSummary;
    private Instant resolutionTimestamp;
    private Long version;
    private List<CaseNoteDto> notes;
    private Instant createdAt;
    private Instant updatedAt;

    public static ExceptionCaseDto fromEntity(ExceptionCase ec) {
        return ExceptionCaseDto.builder()
                .id(ec.getId())
                .caseNumber(ec.getCaseNumber())
                .transaction(ec.getTransaction() != null ? TransactionDto.fromEntity(ec.getTransaction()) : null)
                .ruleId(ec.getRule() != null ? ec.getRule().getId() : null)
                .ruleCode(ec.getRule() != null ? ec.getRule().getRuleCode() : null)
                .category(ec.getCategory())
                .severity(ec.getSeverity())
                .status(ec.getStatus())
                .assignedAnalystId(ec.getAssignedAnalyst() != null ? ec.getAssignedAnalyst().getId() : null)
                .assignedAnalystUsername(ec.getAssignedAnalyst() != null ? ec.getAssignedAnalyst().getUsername() : null)
                .assignedAnalystFullName(ec.getAssignedAnalyst() != null ? ec.getAssignedAnalyst().getFullName() : null)
                .resolutionSummary(ec.getResolutionSummary())
                .resolutionTimestamp(ec.getResolutionTimestamp())
                .version(ec.getVersion())
                .notes(ec.getNotes() != null ? ec.getNotes().stream().map(CaseNoteDto::fromEntity).collect(Collectors.toList()) : List.of())
                .createdAt(ec.getCreatedAt())
                .updatedAt(ec.getUpdatedAt())
                .build();
    }
}

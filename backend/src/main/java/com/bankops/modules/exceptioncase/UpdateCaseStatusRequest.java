package com.bankops.modules.exceptioncase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCaseStatusRequest {

    @NotBlank(message = "Target status is required")
    @Pattern(regexp = "OPEN|IN_REVIEW|RESOLVED|REOPENED", message = "Target status must be OPEN, IN_REVIEW, RESOLVED, or REOPENED")
    private String targetStatus;

    private String resolutionSummary;

    private String justification;

    /**
     * Expected version for optimistic locking.
     */
    private Long expectedVersion;
}

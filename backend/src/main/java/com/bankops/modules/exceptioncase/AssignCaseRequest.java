package com.bankops.modules.exceptioncase;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCaseRequest {

    @NotNull(message = "Assigned analyst user ID is required")
    private UUID analystId;

    /**
     * Expected version for optimistic locking.
     */
    private Long expectedVersion;
}

package com.bankops.modules.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleExecutionResult {
    private boolean matched;
    private ValidationRule rule;
    private String category;
    private String severity;
    private String failureReason;

    public static RuleExecutionResult notMatched() {
        return RuleExecutionResult.builder().matched(false).build();
    }

    public static RuleExecutionResult matched(ValidationRule rule, String reason) {
        return RuleExecutionResult.builder()
                .matched(true)
                .rule(rule)
                .category(rule.getCategory())
                .severity(rule.getSeverity())
                .failureReason(reason)
                .build();
    }
}

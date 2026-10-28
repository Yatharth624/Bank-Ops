package com.bankops.modules.rule;

import com.bankops.common.DuplicateResourceException;
import com.bankops.common.ResourceNotFoundException;
import com.bankops.modules.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationRuleService {

    private final ValidationRuleRepository ruleRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<RuleDto> getAllRules() {
        return ruleRepository.findAll().stream()
                .map(RuleDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ValidationRule> getActiveRules() {
        return ruleRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public RuleDto getRuleById(UUID id) {
        return ruleRepository.findById(id)
                .map(RuleDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("ValidationRule", "id", id));
    }

    @Transactional
    public RuleDto createRule(CreateRuleRequest request) {
        if (ruleRepository.existsByRuleCode(request.getRuleCode())) {
            throw new DuplicateResourceException("Validation rule with code '" + request.getRuleCode() + "' already exists");
        }

        ValidationRule rule = ValidationRule.builder()
                .ruleCode(request.getRuleCode().trim().toUpperCase())
                .ruleName(request.getRuleName().trim())
                .description(request.getDescription())
                .category(request.getCategory().trim().toUpperCase())
                .severity(request.getSeverity().trim().toUpperCase())
                .parameters(request.getParameters())
                .active(request.isActive())
                .build();

        ValidationRule saved = ruleRepository.save(rule);

        auditService.logEvent("CREATE_RULE", "VALIDATION_RULE", saved.getId().toString(),
                String.format("Created validation rule '%s' with severity '%s'", saved.getRuleCode(), saved.getSeverity()), "SUCCESS");

        return RuleDto.fromEntity(saved);
    }

    @Transactional
    public RuleDto updateRule(UUID id, UpdateRuleRequest request) {
        ValidationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ValidationRule", "id", id));

        if (request.getRuleName() != null) rule.setRuleName(request.getRuleName().trim());
        if (request.getDescription() != null) rule.setDescription(request.getDescription());
        if (request.getCategory() != null) rule.setCategory(request.getCategory().trim().toUpperCase());
        if (request.getSeverity() != null) rule.setSeverity(request.getSeverity().trim().toUpperCase());
        if (request.getParameters() != null) rule.setParameters(request.getParameters());
        if (request.getActive() != null) rule.setActive(request.getActive());

        ValidationRule updated = ruleRepository.save(rule);

        auditService.logEvent("UPDATE_RULE", "VALIDATION_RULE", updated.getId().toString(),
                String.format("Updated validation rule '%s' configuration", updated.getRuleCode()), "SUCCESS");

        return RuleDto.fromEntity(updated);
    }
}

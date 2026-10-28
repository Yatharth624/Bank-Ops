package com.bankops.modules.rule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ValidationRuleRepository extends JpaRepository<ValidationRule, UUID> {
    Optional<ValidationRule> findByRuleCode(String ruleCode);
    List<ValidationRule> findByActiveTrue();
    boolean existsByRuleCode(String ruleCode);
}

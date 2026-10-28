package com.bankops.modules.rule;

import com.bankops.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Rule Administration", description = "Endpoints for configuring operational validation rules")
public class ValidationRuleController {

    private final ValidationRuleService ruleService;

    @GetMapping
    @Operation(summary = "List all validation rules")
    public ResponseEntity<ApiResponse<List<RuleDto>>> getAllRules() {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getAllRules(), "Rules retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get validation rule by ID")
    public ResponseEntity<ApiResponse<RuleDto>> getRuleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getRuleById(id), "Rule retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create a new validation rule")
    public ResponseEntity<ApiResponse<RuleDto>> createRule(@Valid @RequestBody CreateRuleRequest request) {
        RuleDto created = ruleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Validation rule created successfully"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update an existing validation rule")
    public ResponseEntity<ApiResponse<RuleDto>> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRuleRequest request) {
        RuleDto updated = ruleService.updateRule(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Validation rule updated successfully"));
    }
}

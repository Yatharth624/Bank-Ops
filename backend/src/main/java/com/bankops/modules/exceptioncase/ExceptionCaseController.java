package com.bankops.modules.exceptioncase;

import com.bankops.common.ApiResponse;
import com.bankops.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exceptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
@Tag(name = "Exception & Case Management", description = "Endpoints for managing operational exceptions, workflow transitions, notes, and timelines")
public class ExceptionCaseController {

    private final ExceptionCaseService caseService;

    @GetMapping
    @Operation(summary = "Search and filter operational exception cases")
    public ResponseEntity<ApiResponse<PagedResponse<ExceptionCaseDto>>> searchCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID assignedAnalystId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        var page = caseService.searchCases(status, severity, category, assignedAnalystId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page), "Cases retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exception case details by ID")
    public ResponseEntity<ApiResponse<ExceptionCaseDto>> getCaseById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(caseService.getCaseById(id), "Case details retrieved"));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign or reassign an exception case to an analyst")
    public ResponseEntity<ApiResponse<ExceptionCaseDto>> assignCase(
            @PathVariable UUID id,
            @Valid @RequestBody AssignCaseRequest request) {
        ExceptionCaseDto updated = caseService.assignCase(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Case assigned successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Perform a validated workflow status transition")
    public ResponseEntity<ApiResponse<ExceptionCaseDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCaseStatusRequest request) {
        ExceptionCaseDto updated = caseService.updateCaseStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Case status updated successfully"));
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Add an investigation note to a case")
    public ResponseEntity<ApiResponse<CaseNoteDto>> addNote(
            @PathVariable UUID id,
            @Valid @RequestBody AddCaseNoteRequest request) {
        CaseNoteDto note = caseService.addCaseNote(id, request);
        return ResponseEntity.ok(ApiResponse.success(note, "Investigation note recorded"));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Get chronological audit and investigation timeline for a case")
    public ResponseEntity<ApiResponse<List<CaseTimelineEventDto>>> getTimeline(@PathVariable UUID id) {
        List<CaseTimelineEventDto> timeline = caseService.getCaseTimeline(id);
        return ResponseEntity.ok(ApiResponse.success(timeline, "Case timeline retrieved"));
    }
}

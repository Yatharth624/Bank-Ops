package com.bankops.modules.exceptioncase;

import com.bankops.common.ConcurrencyConflictException;
import com.bankops.common.InvalidWorkflowTransitionException;
import com.bankops.common.ResourceNotFoundException;
import com.bankops.modules.audit.AuditService;
import com.bankops.modules.rule.ValidationRule;
import com.bankops.modules.transaction.Transaction;
import com.bankops.modules.user.User;
import com.bankops.modules.user.UserRepository;
import com.bankops.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionCaseService {

    private final ExceptionCaseRepository caseRepository;
    private final CaseNoteRepository caseNoteRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private static final AtomicLong CASE_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Transactional
    public Optional<ExceptionCase> createCaseForTransaction(Transaction tx, ValidationRule rule, String failureReason) {
        if (rule != null && caseRepository.existsByTransactionIdAndRuleId(tx.getId(), rule.getId())) {
            log.info("Duplicate case creation suppressed for tx '{}' and rule '{}'",
                    tx.getExternalReference(), rule.getRuleCode());
            return Optional.empty();
        }

        String caseNumber = String.format("CASE-%d-%04d",
                Instant.now().getEpochSecond() % 1000000,
                CASE_SEQ.incrementAndGet() % 10000);

        ExceptionCase ec = ExceptionCase.builder()
                .caseNumber(caseNumber)
                .transaction(tx)
                .rule(rule)
                .category(rule != null ? rule.getCategory() : "GENERAL_EXCEPTION")
                .severity(rule != null ? rule.getSeverity() : "MEDIUM")
                .status("OPEN")
                .version(0L)
                .build();

        ExceptionCase saved = caseRepository.save(ec);

        auditService.logEvent("CREATE_EXCEPTION_CASE", "EXCEPTION_CASE", saved.getId().toString(),
                String.format("Auto-generated exception case %s: %s", caseNumber, failureReason), "SUCCESS");

        return Optional.of(saved);
    }

    @Transactional(readOnly = true)
    public Page<ExceptionCaseDto> searchCases(
            String status, String severity, String category, UUID assignedAnalystId, String search, Pageable pageable) {
        return caseRepository.searchCases(status, severity, category, assignedAnalystId, search, pageable)
                .map(ExceptionCaseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public ExceptionCaseDto getCaseById(UUID id) {
        ExceptionCase ec = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExceptionCase", "id", id));
        return ExceptionCaseDto.fromEntity(ec);
    }

    @Transactional(readOnly = true)
    public List<ExceptionCaseDto> getCasesForTransaction(UUID transactionId) {
        return caseRepository.findByTransactionId(transactionId).stream()
                .map(ExceptionCaseDto::fromEntity)
                .toList();
    }

    @Transactional
    public ExceptionCaseDto assignCase(UUID caseId, AssignCaseRequest request) {
        ExceptionCase ec = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ExceptionCase", "id", caseId));

        if (request.getExpectedVersion() != null && !ec.getVersion().equals(request.getExpectedVersion())) {
            throw new ConcurrencyConflictException("Case has been updated concurrently. Please refresh.");
        }

        User analyst = userRepository.findById(request.getAnalystId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAnalystId()));

        User currentActor = getCurrentUserOrSystem();

        ec.setAssignedAnalyst(analyst);
        if ("OPEN".equals(ec.getStatus())) {
            ec.setStatus("IN_REVIEW");
        }

        ExceptionCase updated = caseRepository.save(ec);

        CaseNote note = CaseNote.builder()
                .exceptionCase(updated)
                .author(currentActor)
                .noteText(String.format("Case assigned to analyst: %s (%s)", analyst.getFullName(), analyst.getUsername()))
                .build();
        caseNoteRepository.save(note);

        auditService.logEvent("ASSIGN_CASE", "EXCEPTION_CASE", updated.getId().toString(),
                String.format("Assigned case %s to %s", updated.getCaseNumber(), analyst.getUsername()), "SUCCESS");

        return ExceptionCaseDto.fromEntity(updated);
    }

    @Transactional
    public ExceptionCaseDto updateCaseStatus(UUID caseId, UpdateCaseStatusRequest request) {
        ExceptionCase ec = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ExceptionCase", "id", caseId));

        if (request.getExpectedVersion() != null && !ec.getVersion().equals(request.getExpectedVersion())) {
            throw new ConcurrencyConflictException("Case has been updated concurrently. Please refresh.");
        }

        String currentStatus = ec.getStatus();
        String targetStatus = request.getTargetStatus().toUpperCase();

        validateStatusTransition(currentStatus, targetStatus, request);

        ec.setStatus(targetStatus);
        if ("RESOLVED".equals(targetStatus)) {
            ec.setResolutionSummary(request.getResolutionSummary());
            ec.setResolutionTimestamp(Instant.now());
        } else if ("REOPENED".equals(targetStatus)) {
            ec.setResolutionTimestamp(null);
        }

        ExceptionCase updated = caseRepository.save(ec);
        User currentActor = getCurrentUserOrSystem();

        String noteContent = String.format("Status transitioned from '%s' to '%s'. %s",
                currentStatus, targetStatus,
                request.getJustification() != null ? "Justification: " + request.getJustification() :
                (request.getResolutionSummary() != null ? "Resolution: " + request.getResolutionSummary() : ""));

        CaseNote note = CaseNote.builder()
                .exceptionCase(updated)
                .author(currentActor)
                .noteText(noteContent)
                .build();
        caseNoteRepository.save(note);

        auditService.logEvent("STATUS_TRANSITION", "EXCEPTION_CASE", updated.getId().toString(),
                String.format("Case %s status transitioned from %s to %s", updated.getCaseNumber(), currentStatus, targetStatus), "SUCCESS");

        return ExceptionCaseDto.fromEntity(updated);
    }

    @Transactional
    public CaseNoteDto addCaseNote(UUID caseId, AddCaseNoteRequest request) {
        ExceptionCase ec = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ExceptionCase", "id", caseId));

        User currentActor = getCurrentUserOrSystem();

        CaseNote note = CaseNote.builder()
                .exceptionCase(ec)
                .author(currentActor)
                .noteText(request.getNoteText().trim())
                .build();

        CaseNote saved = caseNoteRepository.save(note);

        auditService.logEvent("ADD_CASE_NOTE", "EXCEPTION_CASE", ec.getId().toString(),
                String.format("Note added to case %s by %s", ec.getCaseNumber(), currentActor.getUsername()), "SUCCESS");

        return CaseNoteDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CaseTimelineEventDto> getCaseTimeline(UUID caseId) {
        ExceptionCase ec = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ExceptionCase", "id", caseId));

        List<CaseTimelineEventDto> timeline = new ArrayList<>();

        // Case creation
        timeline.add(CaseTimelineEventDto.builder()
                .eventType("CASE_CREATED")
                .actorUsername("SYSTEM")
                .description("Exception case registered from transaction rule validation")
                .timestamp(ec.getCreatedAt())
                .build());

        // Notes and status transitions
        List<CaseNote> notes = caseNoteRepository.findByExceptionCaseIdOrderByCreatedAtAsc(caseId);
        for (CaseNote n : notes) {
            timeline.add(CaseTimelineEventDto.builder()
                    .eventType("NOTE_OR_TRANSITION")
                    .actorUsername(n.getAuthor() != null ? n.getAuthor().getUsername() : "UNKNOWN")
                    .description(n.getNoteText())
                    .timestamp(n.getCreatedAt())
                    .build());
        }

        timeline.sort(Comparator.comparing(CaseTimelineEventDto::getTimestamp));
        return timeline;
    }

    private void validateStatusTransition(String current, String target, UpdateCaseStatusRequest req) {
        if (current.equals(target)) {
            throw new InvalidWorkflowTransitionException(current, target, "Case is already in status " + target);
        }

        switch (current) {
            case "OPEN":
                if ("IN_REVIEW".equals(target)) return;
                if ("RESOLVED".equals(target)) {
                    if (req.getResolutionSummary() == null || req.getResolutionSummary().isBlank()) {
                        throw new InvalidWorkflowTransitionException(current, target, "Resolution summary is required to resolve a case.");
                    }
                    return;
                }
                break;

            case "IN_REVIEW":
                if ("RESOLVED".equals(target)) {
                    if (req.getResolutionSummary() == null || req.getResolutionSummary().isBlank()) {
                        throw new InvalidWorkflowTransitionException(current, target, "Resolution summary is required to resolve a case.");
                    }
                    return;
                }
                if ("OPEN".equals(target)) return; // Unassign / return to triage queue
                break;

            case "RESOLVED":
                if ("REOPENED".equals(target)) {
                    // Reopening a resolved case requires ADMIN privilege and justification
                    boolean isAdmin = SecurityUtils.hasRole("ROLE_ADMIN");
                    if (!isAdmin) {
                        throw new InvalidWorkflowTransitionException(current, target, "Only users with ROLE_ADMIN are authorized to reopen a resolved case.");
                    }
                    if (req.getJustification() == null || req.getJustification().isBlank()) {
                        throw new InvalidWorkflowTransitionException(current, target, "A documented justification is mandatory when reopening a resolved case.");
                    }
                    return;
                }
                break;

            case "REOPENED":
                if ("IN_REVIEW".equals(target) || "RESOLVED".equals(target)) return;
                break;

            default:
                break;
        }

        throw new InvalidWorkflowTransitionException(current, target,
                String.format("Transition from %s to %s is prohibited by operational workflow policy.", current, target));
    }

    private User getCurrentUserOrSystem() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("No default user found")));
    }
}

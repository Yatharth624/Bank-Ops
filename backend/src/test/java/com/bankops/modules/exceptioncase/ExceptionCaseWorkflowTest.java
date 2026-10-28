package com.bankops.modules.exceptioncase;

import com.bankops.common.ConcurrencyConflictException;
import com.bankops.common.InvalidWorkflowTransitionException;
import com.bankops.modules.audit.AuditService;
import com.bankops.modules.transaction.Transaction;
import com.bankops.modules.user.Role;
import com.bankops.modules.user.User;
import com.bankops.modules.user.UserRepository;
import com.bankops.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExceptionCaseWorkflowTest {

    private ExceptionCaseRepository caseRepository;
    private CaseNoteRepository caseNoteRepository;
    private UserRepository userRepository;
    private AuditService auditService;
    private ExceptionCaseService caseService;

    private User analystUser;
    private User adminUser;
    private ExceptionCase openCase;

    @BeforeEach
    void setUp() {
        caseRepository = Mockito.mock(ExceptionCaseRepository.class);
        caseNoteRepository = Mockito.mock(CaseNoteRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        auditService = Mockito.mock(AuditService.class);
        caseService = new ExceptionCaseService(caseRepository, caseNoteRepository, userRepository, auditService);

        Role analystRole = Role.builder().id(UUID.randomUUID()).name("ROLE_ANALYST").build();
        Role adminRole = Role.builder().id(UUID.randomUUID()).name("ROLE_ADMIN").build();

        analystUser = User.builder()
                .id(UUID.randomUUID())
                .username("analyst_david")
                .fullName("David Chen")
                .email("dchen@bankops.internal")
                .active(true)
                .roles(Set.of(analystRole))
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin_sarah")
                .fullName("Sarah Jenkins")
                .email("sjenkins@bankops.internal")
                .active(true)
                .roles(Set.of(adminRole, analystRole))
                .build();

        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID())
                .externalReference("TX-TEST-01")
                .accountReference("ACT-01")
                .amount(new BigDecimal("60000.00"))
                .currency("USD")
                .transactionType("WIRE_TRANSFER")
                .channel("BRANCH")
                .status("COMPLETED")
                .transactionTimestamp(Instant.now())
                .build();

        openCase = ExceptionCase.builder()
                .id(UUID.randomUUID())
                .caseNumber("CASE-2026-0099")
                .transaction(tx)
                .category("HIGH_VALUE_BREACH")
                .severity("HIGH")
                .status("OPEN")
                .version(1L)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Should successfully transition OPEN to IN_REVIEW")
    void shouldTransitionOpenToInReview() {
        authenticateAs(analystUser);
        when(caseRepository.findById(openCase.getId())).thenReturn(Optional.of(openCase));
        when(caseRepository.save(any(ExceptionCase.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByUsername("analyst_david")).thenReturn(Optional.of(analystUser));

        UpdateCaseStatusRequest req = UpdateCaseStatusRequest.builder()
                .targetStatus("IN_REVIEW")
                .justification("Starting initial investigation")
                .build();

        ExceptionCaseDto result = caseService.updateCaseStatus(openCase.getId(), req);

        assertThat(result.getStatus()).isEqualTo("IN_REVIEW");
        verify(auditService).logEvent(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should transition IN_REVIEW to RESOLVED when resolution summary is provided")
    void shouldTransitionInReviewToResolved() {
        authenticateAs(analystUser);
        openCase.setStatus("IN_REVIEW");
        when(caseRepository.findById(openCase.getId())).thenReturn(Optional.of(openCase));
        when(caseRepository.save(any(ExceptionCase.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByUsername("analyst_david")).thenReturn(Optional.of(analystUser));

        UpdateCaseStatusRequest req = UpdateCaseStatusRequest.builder()
                .targetStatus("RESOLVED")
                .resolutionSummary("Verified customer ID and corporate signatory on file. Operational risk cleared.")
                .build();

        ExceptionCaseDto result = caseService.updateCaseStatus(openCase.getId(), req);

        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(result.getResolutionSummary()).isNotBlank();
        assertThat(result.getResolutionTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should reject transition to RESOLVED if resolution summary is omitted")
    void shouldRejectResolveWithoutSummary() {
        authenticateAs(analystUser);
        openCase.setStatus("IN_REVIEW");
        when(caseRepository.findById(openCase.getId())).thenReturn(Optional.of(openCase));

        UpdateCaseStatusRequest req = UpdateCaseStatusRequest.builder()
                .targetStatus("RESOLVED")
                .resolutionSummary("")
                .build();

        assertThatThrownBy(() -> caseService.updateCaseStatus(openCase.getId(), req))
                .isInstanceOf(InvalidWorkflowTransitionException.class)
                .hasMessageContaining("Resolution summary is required");
    }

    @Test
    @DisplayName("Should reject REOPEN transition when attempted by non-admin analyst")
    void shouldRejectReopenByAnalyst() {
        authenticateAs(analystUser);
        openCase.setStatus("RESOLVED");
        when(caseRepository.findById(openCase.getId())).thenReturn(Optional.of(openCase));

        UpdateCaseStatusRequest req = UpdateCaseStatusRequest.builder()
                .targetStatus("REOPENED")
                .justification("Customer called with new dispute")
                .build();

        assertThatThrownBy(() -> caseService.updateCaseStatus(openCase.getId(), req))
                .isInstanceOf(InvalidWorkflowTransitionException.class)
                .hasMessageContaining("Only users with ROLE_ADMIN are authorized");
    }

    @Test
    @DisplayName("Should allow REOPEN transition when authorized admin provides justification")
    void shouldAllowReopenByAdminWithJustification() {
        authenticateAs(adminUser);
        openCase.setStatus("RESOLVED");
        when(caseRepository.findById(openCase.getId())).thenReturn(Optional.of(openCase));
        when(caseRepository.save(any(ExceptionCase.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByUsername("admin_sarah")).thenReturn(Optional.of(adminUser));

        UpdateCaseStatusRequest req = UpdateCaseStatusRequest.builder()
                .targetStatus("REOPENED")
                .justification("Escalated review required by compliance audit")
                .build();

        ExceptionCaseDto result = caseService.updateCaseStatus(openCase.getId(), req);

        assertThat(result.getStatus()).isEqualTo("REOPENED");
        assertThat(result.getResolutionTimestamp()).isNull();
    }

    @Test
    @DisplayName("Should throw ConcurrencyConflictException when expected version does not match")
    void shouldEnforceOptimisticLockingCheck() {
        authenticateAs(analystUser);
        openCase.setVersion(2L);
        when(caseRepository.findById(openCase.getId())).thenReturn(Optional.of(openCase));

        UpdateCaseStatusRequest req = UpdateCaseStatusRequest.builder()
                .targetStatus("IN_REVIEW")
                .expectedVersion(1L)
                .build();

        assertThatThrownBy(() -> caseService.updateCaseStatus(openCase.getId(), req))
                .isInstanceOf(ConcurrencyConflictException.class)
                .hasMessageContaining("updated concurrently");
    }
}

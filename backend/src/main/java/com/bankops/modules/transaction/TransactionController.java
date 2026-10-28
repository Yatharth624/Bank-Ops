package com.bankops.modules.transaction;

import com.bankops.common.ApiResponse;
import com.bankops.common.PagedResponse;
import com.bankops.modules.exceptioncase.ExceptionCaseDto;
import com.bankops.modules.exceptioncase.ExceptionCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
@Tag(name = "Transaction Monitoring", description = "Endpoints for synthetic banking transaction ingestion, querying, and monitoring")
public class TransactionController {

    private final TransactionService transactionService;
    private final ExceptionCaseService caseService;

    @GetMapping
    @Operation(summary = "Search, filter, and paginate synthetic transactions")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionDto>>> getTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "transactionTimestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        var page = transactionService.searchTransactions(
                status, transactionType, channel, minAmount, maxAmount, fromDate, toDate, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page), "Transactions retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details by ID")
    public ResponseEntity<ApiResponse<TransactionDto>> getTransactionById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionById(id), "Transaction details retrieved"));
    }

    @GetMapping("/{id}/exceptions")
    @Operation(summary = "Get operational exceptions triggered by a transaction")
    public ResponseEntity<ApiResponse<List<ExceptionCaseDto>>> getExceptionsForTransaction(@PathVariable UUID id) {
        List<ExceptionCaseDto> cases = caseService.getCasesForTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(cases, "Associated exception cases retrieved"));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Batch CSV ingestion of synthetic transactions with row-level validation and rule evaluation")
    public ResponseEntity<ApiResponse<TransactionImportResultDto>> importTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.success(null, "Uploaded CSV file is empty"));
        }

        TransactionImportResultDto result = transactionService.importTransactionsCsv(file.getInputStream(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(result, "CSV ingestion processed successfully"));
    }
}

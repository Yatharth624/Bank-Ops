package com.bankops.modules.dashboard;

import com.bankops.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
@Tag(name = "Operational Dashboard", description = "Endpoints for aggregated operational KPIs, exception distributions, and trends")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get aggregated operational KPI metrics and category/severity distributions")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSummary(), "Dashboard summary metrics retrieved"));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get historical transaction and exception trends over the specified window (days)")
    public ResponseEntity<ApiResponse<List<TrendPointDto>>> getTrends(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getTrends(days), "Operational trends retrieved"));
    }
}

package com.fillthepockets.controller;

import com.fillthepockets.dto.ApiResponse;
import com.fillthepockets.dto.EmployeeDashboardStatsResponse;
import com.fillthepockets.dto.RepairStatusUpdateRequest;
import com.fillthepockets.dto.RoadReportResponse;
import com.fillthepockets.entity.RepairStatus;
import com.fillthepockets.entity.RepairStatusUpdate;
import com.fillthepockets.entity.ReportStatus;
import com.fillthepockets.entity.User;
import com.fillthepockets.repository.UserRepository;
import com.fillthepockets.security.UserDetailsImpl;
import com.fillthepockets.service.ReportService;
import com.fillthepockets.service.StatsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee")
@PreAuthorize("hasRole('PANCHAYAT_EMPLOYEE')")
public class EmployeeController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private StatsService statsService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<EmployeeDashboardStatsResponse>> getDashboardStats() {
        EmployeeDashboardStatsResponse stats = statsService.getEmployeeDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok("Employee dashboard statistics", stats));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<RoadReportResponse>>> getReports(
            @RequestParam(value = "status", required = false) ReportStatus status,
            @RequestParam(value = "repairStatus", required = false) RepairStatus repairStatus,
            @RequestParam(value = "keyword", required = false) String keyword) {

        List<RoadReportResponse> reports = reportService.getReportsForEmployee(status, repairStatus, keyword);
        return ResponseEntity.ok(ApiResponse.ok("Road damage reports retrieved", reports));
    }

    @PostMapping("/reports/{id}/update-repair-status")
    public ResponseEntity<ApiResponse<RoadReportResponse>> updateRepairStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody RepairStatusUpdateRequest req,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            User employee = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Employee user not found"));

            RoadReportResponse updated = reportService.updateRepairStatus(id, employee, req);
            return ResponseEntity.ok(ApiResponse.ok("Road repair progress updated successfully.", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/reports/{id}/repair-history")
    public ResponseEntity<ApiResponse<List<RepairStatusUpdate>>> getRepairHistory(@PathVariable("id") Long id) {
        try {
            List<RepairStatusUpdate> history = reportService.getRepairHistoryForReport(id);
            return ResponseEntity.ok(ApiResponse.ok("Repair audit history", history));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/villagers/{villagerId}/summary")
    public ResponseEntity<ApiResponse<com.fillthepockets.dto.VillagerDashboardResponse>> getVillagerSummary(@PathVariable("villagerId") Long villagerId) {
        try {
            User villager = userRepository.findById(villagerId)
                    .orElseThrow(() -> new IllegalArgumentException("Villager not found with ID: " + villagerId));
            com.fillthepockets.dto.VillagerDashboardResponse resp = statsService.getVillagerDashboard(villager);
            return ResponseEntity.ok(ApiResponse.ok("Villager summary and rewards retrieved", resp));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

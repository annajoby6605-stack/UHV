package com.fillthepockets.service;

import com.fillthepockets.dto.EmployeeDashboardStatsResponse;
import com.fillthepockets.dto.PublicStatsResponse;
import com.fillthepockets.dto.VillagerDashboardResponse;
import com.fillthepockets.entity.*;
import com.fillthepockets.repository.PointsTransactionRepository;
import com.fillthepockets.repository.RoadReportRepository;
import com.fillthepockets.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    @Autowired
    private RoadReportRepository roadReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointsTransactionRepository pointsTransactionRepository;

    @Autowired
    private ReportService reportService;

    @Transactional(readOnly = true)
    public PublicStatsResponse getPublicStats() {
        long total = roadReportRepository.count();
        long accepted = roadReportRepository.countByStatus(ReportStatus.ACCEPTED);
        long resolved = roadReportRepository.countByRepairStatus(RepairStatus.RESOLVED);
        long pending = roadReportRepository.countByRepairStatus(RepairStatus.PENDING);
        long points = pointsTransactionRepository.sumTotalPointsAwarded();
        long villagers = userRepository.countByRole(Role.VILLAGER);

        return new PublicStatsResponse(total, accepted, resolved, pending, points, villagers);
    }

    @Transactional(readOnly = true)
    public EmployeeDashboardStatsResponse getEmployeeDashboardStats() {
        long total = roadReportRepository.count();
        long accepted = roadReportRepository.countByStatus(ReportStatus.ACCEPTED);
        long rejectedUnrelated = roadReportRepository.countByStatus(ReportStatus.REJECTED_UNRELATED);
        long rejectedDuplicate = roadReportRepository.countByStatus(ReportStatus.REJECTED_DUPLICATE);
        long rejected = rejectedUnrelated + rejectedDuplicate;

        long pending = roadReportRepository.countByRepairStatus(RepairStatus.PENDING);
        long inProgress = roadReportRepository.countByRepairStatus(RepairStatus.IN_PROGRESS);
        long resolved = roadReportRepository.countByRepairStatus(RepairStatus.RESOLVED);

        long villagersReporting = roadReportRepository.countDistinctVillagersWithReports();
        long pointsAwarded = pointsTransactionRepository.sumTotalPointsAwarded();
        long eligibleReports = accepted;

        return new EmployeeDashboardStatsResponse(total, accepted, rejected, pending, inProgress, resolved,
                villagersReporting, pointsAwarded, eligibleReports);
    }

    @Transactional(readOnly = true)
    public VillagerDashboardResponse getVillagerDashboard(User villager) {
        // Re-read fresh user balance
        User fresh = userRepository.findById(villager.getId()).orElse(villager);

        long submitted = roadReportRepository.countByVillager(fresh);
        long accepted = roadReportRepository.countByVillagerAndStatus(fresh, ReportStatus.ACCEPTED);
        long rejectedUnrelated = roadReportRepository.countByVillagerAndStatus(fresh, ReportStatus.REJECTED_UNRELATED);
        long rejectedDuplicate = roadReportRepository.countByVillagerAndStatus(fresh, ReportStatus.REJECTED_DUPLICATE);
        long pending = roadReportRepository.countByVillagerAndRepairStatus(fresh, RepairStatus.PENDING);
        long inProgress = roadReportRepository.countByVillagerAndRepairStatus(fresh, RepairStatus.IN_PROGRESS);
        long resolved = roadReportRepository.countByVillagerAndRepairStatus(fresh, RepairStatus.RESOLVED);
        long totalPointsEarned = pointsTransactionRepository.sumTotalPointsAwardedToUser(fresh);

        VillagerDashboardResponse resp = new VillagerDashboardResponse();
        resp.setFullName(fresh.getFullName());
        resp.setEmail(fresh.getEmail());
        resp.setPointsBalance(fresh.getPointsBalance());
        resp.setTotalSubmitted(submitted);
        resp.setTotalAccepted(accepted);
        long totalRejected = rejectedUnrelated + rejectedDuplicate;
        resp.setTotalRejected(totalRejected);
        resp.setPendingReports(pending);
        resp.setInProgressReports(inProgress);
        resp.setResolvedReports(resolved);
        resp.setTotalPointsEarned(totalPointsEarned);
        resp.setEligibleForRewards(fresh.getPointsBalance() >= 40); // 40 pts threshold for first coupon
        resp.setRecentReports(reportService.getVillagerReports(fresh));

        return resp;
    }
}

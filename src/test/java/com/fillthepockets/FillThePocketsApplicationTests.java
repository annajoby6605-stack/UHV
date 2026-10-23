package com.fillthepockets;

import com.fillthepockets.dto.*;
import com.fillthepockets.entity.*;
import com.fillthepockets.repository.*;
import com.fillthepockets.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "app.jwt.secret=test-only-signing-key-for-fill-the-pockets-tests")
public class FillThePocketsApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmployeeVerificationService employeeVerificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AiImageVerificationService aiImageVerificationService;

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;

    @Autowired
    private PointsService pointsService;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private StatsService statsService;

    // Helper: generate synthetic asphalt pothole image for testing
    private byte[] createTestRoadImage(boolean isPothole) throws Exception {
        BufferedImage img = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        if (isPothole) {
            // Asphalt background: dark gray (RGB ~ 60, 60, 60) with texture
            g.setColor(new Color(65, 65, 65));
            g.fillRect(0, 0, 300, 300);

            // Add road aggregate speckles
            for (int i = 0; i < 400; i++) {
                int gray = 40 + (int)(Math.random() * 50);
                g.setColor(new Color(gray, gray, gray));
                int x = (int)(Math.random() * 300);
                int y = (int)(Math.random() * 300);
                g.fillRect(x, y, 2, 2);
            }

            // Draw a distinct dark cavity / pothole in center
            g.setColor(new Color(25, 25, 25));
            g.fillOval(80, 80, 140, 120);

            // Rough fracture cracks around the cavity
            g.setColor(new Color(15, 15, 15));
            g.setStroke(new BasicStroke(2.5f));
            g.drawArc(70, 70, 160, 140, 0, 360);
            g.drawLine(220, 140, 270, 130);
            g.drawLine(80, 140, 30, 160);
        } else {
            // Unrelated image: Bright colorful indoor/floral scene (Green and Pink)
            g.setColor(new Color(240, 200, 210)); // Bright pink/skin tone
            g.fillRect(0, 0, 300, 300);
            g.setColor(new Color(34, 197, 94)); // Lush green
            g.fillOval(50, 50, 200, 200);
            g.setColor(new Color(239, 68, 68)); // Vivid red flower
            g.fillOval(100, 100, 100, 100);
        }

        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Test 1: Verify 5 sample Panchayat Employee demo accounts exist with correct details")
    public void testFiveDemoEmployeesExist() {
        String[][] expectedEmployees = {
            {"EMP001", "Arun Kumar", "arun.kumar@fillthepockets.demo"},
            {"EMP002", "Anjali Mohan", "anjali.mohan@fillthepockets.demo"},
            {"EMP003", "Rahul Joseph", "rahul.joseph@fillthepockets.demo"},
            {"EMP004", "Meera Thomas", "meera.thomas@fillthepockets.demo"},
            {"EMP005", "Nikhil Raj", "nikhil.raj@fillthepockets.demo"}
        };

        for (String[] emp : expectedEmployees) {
            String code = emp[0];
            String name = emp[1];
            String email = emp[2];

            User user = userRepository.findByEmail(email).orElse(null);
            assertNotNull(user, "Employee account " + email + " must exist in database");
            assertEquals(Role.PANCHAYAT_EMPLOYEE, user.getRole(), "Role must be PANCHAYAT_EMPLOYEE");
            assertEquals(name, user.getFullName());

            EmployeeProfile profile = employeeProfileRepository.findByUser(user).orElse(null);
            assertNotNull(profile, "Profile for " + code + " must exist");
            assertEquals(code, profile.getEmployeeCode());
            assertTrue(profile.getIsApproved(), "Demo employee must be approved");
            assertTrue(profile.getIsDemoAccount(), "Must be marked as demo account");
        }
    }

    @Test
    @DisplayName("Test 2: Villager registration and direct login without OTP")
    @Transactional
    public void testVillagerRegistrationAndLogin() {
        String testEmail = "test.citizen." + System.currentTimeMillis() + "@gmail.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Ramesh Citizen");
        reg.setEmail(testEmail);
        reg.setPassword("Password123");
        reg.setPhone("+91 9988776655");

        AuthResponse regResp = authService.registerVillager(reg);
        assertNotNull(regResp);
        assertNotNull(regResp.getToken());
        assertEquals(Role.VILLAGER, regResp.getRole());
        assertEquals(0, regResp.getPointsBalance());

        // Login without OTP
        LoginRequest login = new LoginRequest();
        login.setEmail(testEmail);
        login.setPassword("Password123");
        AuthResponse loginResp = authService.loginVillager(login);
        assertNotNull(loginResp.getToken());
        assertEquals("Ramesh Citizen", loginResp.getFullName());
    }

    @Autowired
    private OtpRecordRepository otpRecordRepository;

    @Test
    @DisplayName("Test 3: Employee 2FA workflow - Credential check, OTP generation, and verification")
    public void testEmployeeTwoFactorFlow() {
        // Step 1: Initiate login for EMP001
        EmployeeOtpResponse initResp = employeeVerificationService.initiateEmployeeLogin(
                "arun.kumar@fillthepockets.demo", "Demo@Emp123"
        );
        assertNotNull(initResp);
        assertTrue(initResp.isDemoMode());
        assertNotNull(initResp.getDemoOtpCode());
        assertEquals(6, initResp.getDemoOtpCode().length());

        // Step 2: Test with incorrect code
        String wrongCode = initResp.getDemoOtpCode().equals("000000") ? "111111" : "000000";
        IllegalArgumentException incorrectEx = assertThrows(IllegalArgumentException.class, () -> {
            employeeVerificationService.verifyEmployeeCode("arun.kumar@fillthepockets.demo", wrongCode);
        });
        assertTrue(incorrectEx.getMessage().contains("Incorrect verification code"), "Must give clear error for incorrect code");

        // Step 3: Test with correct code
        User verifiedEmployee = employeeVerificationService.verifyEmployeeCode(
                "arun.kumar@fillthepockets.demo", initResp.getDemoOtpCode()
        );
        assertNotNull(verifiedEmployee);
        assertEquals(Role.PANCHAYAT_EMPLOYEE, verifiedEmployee.getRole());

        // Step 4: Test code cannot be reused (one-time use security)
        IllegalArgumentException reusedEx = assertThrows(IllegalArgumentException.class, () -> {
            employeeVerificationService.verifyEmployeeCode("arun.kumar@fillthepockets.demo", initResp.getDemoOtpCode());
        });
        assertTrue(reusedEx.getMessage().contains("already been used"), "Must state code was already used");
    }

    @Test
    @DisplayName("Test 4: Security - Villager cannot log in through employee portal")
    @Transactional
    public void testSecurityRoleSeparation() {
        String villagerEmail = "citizen.sec." + System.currentTimeMillis() + "@gmail.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Citizen Security");
        reg.setEmail(villagerEmail);
        reg.setPassword("Password123");
        authService.registerVillager(reg);

        // Villager tries to initiate employee login -> Access Denied
        assertThrows(SecurityException.class, () -> {
            employeeVerificationService.initiateEmployeeLogin(villagerEmail, "Password123");
        });
    }

    @Test
    @DisplayName("Test 5: Automatic AI Photo Verification - Accept road damage and award 20 points")
    @Transactional
    public void testAutomaticAiPhotoVerificationAndPoints() throws Exception {
        String email = "villager.ai." + System.currentTimeMillis() + "@gmail.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Sunita Devi");
        reg.setEmail(email);
        reg.setPassword("Password123");
        authService.registerVillager(reg);
        User villager = userRepository.findByEmail(email).orElseThrow();

        // Create genuine road damage image
        byte[] roadImageBytes = createTestRoadImage(true);
        MockMultipartFile imageFile = new MockMultipartFile("image", "pothole.jpg", "image/jpeg", roadImageBytes);

        RoadReportRequest req = new RoadReportRequest();
        req.setRoadName("Panchayat Link Road");
        req.setVillageArea("Ward 2, Sundarapuram");
        req.setDescription("Large crater in asphalt");
        req.setLatitude(10.8520);
        req.setLongitude(76.2730);
        req.setImage(imageFile);

        RoadReportResponse resp = reportService.submitReport(req, villager);
        assertNotNull(resp);
        assertEquals(ReportStatus.ACCEPTED, resp.getStatus(), "Road damage photo must be ACCEPTED");
        assertEquals(20, resp.getPointsAwarded(), "Must automatically award 20 points");
        assertEquals("ROAD_DAMAGE", resp.getAiCategory());
        assertTrue(resp.getAiConfidenceScore() >= 0.70, "Confidence must meet threshold");

        // Verify balance in database
        User updatedVillager = userRepository.findById(villager.getId()).orElseThrow();
        assertEquals(20, updatedVillager.getPointsBalance(), "Database points balance must be 20");
    }

    @Test
    @DisplayName("Test 6: Automatic AI Photo Verification - Reject unrelated photograph with 0 points")
    @Transactional
    public void testAiRejectionForUnrelatedPhoto() throws Exception {
        String email = "villager.unrelated." + System.currentTimeMillis() + "@gmail.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Mohan Lal");
        reg.setEmail(email);
        reg.setPassword("Password123");
        authService.registerVillager(reg);
        User villager = userRepository.findByEmail(email).orElseThrow();

        // Create unrelated image (bright pink/red indoor floral scene)
        byte[] unrelatedBytes = createTestRoadImage(false);
        MockMultipartFile imageFile = new MockMultipartFile("image", "flower.jpg", "image/jpeg", unrelatedBytes);

        RoadReportRequest req = new RoadReportRequest();
        req.setRoadName("Temple Road");
        req.setVillageArea("Ward 1");
        req.setDescription("Random unrelated picture");
        req.setLatitude(10.8600);
        req.setLongitude(76.2800);
        req.setImage(imageFile);

        RoadReportResponse resp = reportService.submitReport(req, villager);
        assertNotNull(resp);
        assertEquals(ReportStatus.REJECTED_UNRELATED, resp.getStatus(), "Unrelated photo must be REJECTED_UNRELATED");
        assertEquals(0, resp.getPointsAwarded(), "Must award zero points for unrelated photo");

        User updatedVillager = userRepository.findById(villager.getId()).orElseThrow();
        assertEquals(0, updatedVillager.getPointsBalance(), "Points balance must remain 0");
    }

    @Test
    @DisplayName("Test 7: Duplicate Prevention - Detect identical image within 15 meters and reject duplicate points")
    @Transactional
    public void testDuplicatePrevention() throws Exception {
        String email = "villager.dup." + System.currentTimeMillis() + "@gmail.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Geeta Rani");
        reg.setEmail(email);
        reg.setPassword("Password123");
        authService.registerVillager(reg);
        User villager = userRepository.findByEmail(email).orElseThrow();

        byte[] roadImageBytes = createTestRoadImage(true);

        // First submission
        MockMultipartFile file1 = new MockMultipartFile("image", "hole1.jpg", "image/jpeg", roadImageBytes);
        RoadReportRequest req1 = new RoadReportRequest();
        req1.setRoadName("Station Road");
        req1.setVillageArea("Ward 3");
        req1.setLatitude(10.850000);
        req1.setLongitude(76.270000);
        req1.setImage(file1);

        RoadReportResponse resp1 = reportService.submitReport(req1, villager);
        assertEquals(ReportStatus.ACCEPTED, resp1.getStatus());
        assertEquals(20, resp1.getPointsAwarded());

        // Second submission: Same spot (5 meters away) with identical photo
        MockMultipartFile file2 = new MockMultipartFile("image", "hole2.jpg", "image/jpeg", roadImageBytes);
        RoadReportRequest req2 = new RoadReportRequest();
        req2.setRoadName("Station Road");
        req2.setVillageArea("Ward 3");
        req2.setLatitude(10.850030); // ~4 meters difference
        req2.setLongitude(76.270030);
        req2.setImage(file2);

        RoadReportResponse resp2 = reportService.submitReport(req2, villager);
        assertEquals(ReportStatus.REJECTED_DUPLICATE, resp2.getStatus(), "Duplicate report must be REJECTED_DUPLICATE");
        assertEquals(0, resp2.getPointsAwarded(), "Duplicate report must award 0 points");

        // Verify points balance is still 20, NOT 40
        User updatedVillager = userRepository.findById(villager.getId()).orElseThrow();
        assertEquals(20, updatedVillager.getPointsBalance(), "Duplicate submission must not award repeated points");
    }

    @Test
    @DisplayName("Test 8: Employee road repair status update and audit history")
    @Transactional
    public void testEmployeeRepairUpdateWorkflow() throws Exception {
        User employee = userRepository.findByEmail("arun.kumar@fillthepockets.demo").orElseThrow();

        // Create a report
        User villager = userRepository.findByRole(Role.VILLAGER).stream().findFirst().orElseGet(() -> {
            User u = new User("temp.vil@demo.in", "pass", "Temp Vil", null, Role.VILLAGER);
            return userRepository.save(u);
        });

        byte[] roadImageBytes = createTestRoadImage(true);
        MockMultipartFile file = new MockMultipartFile("image", "repair_test.jpg", "image/jpeg", roadImageBytes);

        RoadReportRequest req = new RoadReportRequest();
        req.setRoadName("Canal Bund Road");
        req.setVillageArea("North Ward");
        req.setLatitude(10.8555);
        req.setLongitude(76.2755);
        req.setImage(file);
        RoadReportResponse rep = reportService.submitReport(req, villager);

        // Employee updates status to IN_PROGRESS
        RepairStatusUpdateRequest updateReq1 = new RepairStatusUpdateRequest();
        updateReq1.setNewStatus(RepairStatus.IN_PROGRESS);
        updateReq1.setNotes("Inspection done. Asphalt mixer deployed.");
        RoadReportResponse inProgResp = reportService.updateRepairStatus(rep.getId(), employee, updateReq1);
        assertEquals(RepairStatus.IN_PROGRESS, inProgResp.getRepairStatus());

        // Employee updates status to RESOLVED
        RepairStatusUpdateRequest updateReq2 = new RepairStatusUpdateRequest();
        updateReq2.setNewStatus(RepairStatus.RESOLVED);
        updateReq2.setNotes("Pothole completely filled and compacted.");
        RoadReportResponse resolvedResp = reportService.updateRepairStatus(rep.getId(), employee, updateReq2);
        assertEquals(RepairStatus.RESOLVED, resolvedResp.getRepairStatus());

        // Check audit trail
        List<RepairStatusUpdate> history = reportService.getRepairHistoryForReport(rep.getId());
        assertTrue(history.size() >= 2, "Audit history must contain at least 2 updates");
        assertTrue(history.stream().anyMatch(h -> h.getNewStatus() == RepairStatus.RESOLVED), "Audit must contain RESOLVED status");
        assertTrue(history.stream().anyMatch(h -> h.getNewStatus() == RepairStatus.IN_PROGRESS), "Audit must contain IN_PROGRESS status");
    }

    @Test
    @DisplayName("Test 9: Reward Coupon Redemption")
    @Transactional
    public void testRewardCouponRedemption() {
        User villager = new User("rewards.tester@gmail.com", "pass", "Reward Tester", null, Role.VILLAGER);
        villager.setPointsBalance(100);
        villager = userRepository.save(villager);

        List<RewardCoupon> coupons = rewardService.getActiveCoupons();
        assertFalse(coupons.isEmpty(), "Reward coupons must be seeded");

        RewardCoupon coupon = coupons.get(0); // 40 pts coupon
        RewardRedemption redemption = rewardService.redeemCoupon(coupon.getId(), villager);

        assertNotNull(redemption);
        assertNotNull(redemption.getRedemptionCode());
        assertTrue(redemption.getRedemptionCode().startsWith("FTP-"));
        assertEquals(coupon.getPointsRequired(), redemption.getPointsDeducted());

        User afterRedemption = userRepository.findById(villager.getId()).orElseThrow();
        assertEquals(60, afterRedemption.getPointsBalance(), "Points balance should be 100 - 40 = 60");
    }

    @Test
    @DisplayName("Test 10: Employee access request registers in PENDING status and prevents unapproved login")
    @Transactional
    public void testEmployeeAccessRequestPendingApproval() {
        String testEmpEmail = "applicant.emp." + System.currentTimeMillis() + "@panchayat.gov.in";
        String testEmpCode = "EMP999";

        EmployeeRegisterRequest req = new EmployeeRegisterRequest();
        req.setFullName("Govind Sharma");
        req.setEmployeeCode(testEmpCode);
        req.setEmail(testEmpEmail);
        req.setPassword("Official@123");
        req.setConfirmPassword("Official@123");
        req.setDesignation("Assistant Engineer");
        req.setPanchayatName("Sundarapuram Gram Panchayat");

        authService.requestEmployeeAccess(req);

        User empUser = userRepository.findByEmail(testEmpEmail).orElse(null);
        assertNotNull(empUser, "Employee user record must be persisted");
        assertEquals(Role.PANCHAYAT_EMPLOYEE, empUser.getRole());

        EmployeeProfile profile = employeeProfileRepository.findByUser(empUser).orElse(null);
        assertNotNull(profile, "EmployeeProfile must be created");
        assertFalse(profile.getIsApproved(), "Status must be PENDING (isApproved = false)");
        assertFalse(profile.getIsDemoAccount(), "Must not be a demo account");

        // Attempt login -> Must throw SecurityException because account is not approved
        SecurityException ex = assertThrows(SecurityException.class, () -> {
            employeeVerificationService.initiateEmployeeLogin(testEmpEmail, "Official@123");
        });
        assertTrue(ex.getMessage().contains("pending"), "Error must state that the account is pending approval");
    }

    @Test
    @DisplayName("Test 11: Villager registration fails when confirmPassword does not match password")
    @Transactional
    public void testVillagerRegistrationPasswordMismatch() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Testing Mismatch");
        req.setEmail("mismatch." + System.currentTimeMillis() + "@gmail.com");
        req.setPassword("Password123");
        req.setConfirmPassword("DifferentPassword456");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.registerVillager(req);
        });
        assertTrue(ex.getMessage().contains("do not match"), "Must reject mismatched passwords");
    }

    @Test
    @DisplayName("Test 12: OTP retry limit invalidation and expired code rejection")
    public void testOtpRetryLimitAndExpiry() {
        // 1. Retry limit test on Anjali Mohan
        EmployeeOtpResponse initResp = employeeVerificationService.initiateEmployeeLogin(
                "anjali.mohan@fillthepockets.demo", "Demo@Emp123"
        );
        String correctCode = initResp.getDemoOtpCode();
        String wrongCode = correctCode.equals("000000") ? "111111" : "000000";

        // Attempt 1 incorrect
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            employeeVerificationService.verifyEmployeeCode("anjali.mohan@fillthepockets.demo", wrongCode);
        });
        assertTrue(ex1.getMessage().contains("2 attempt(s) remaining"));

        // Attempt 2 incorrect
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
            employeeVerificationService.verifyEmployeeCode("anjali.mohan@fillthepockets.demo", wrongCode);
        });
        assertTrue(ex2.getMessage().contains("1 attempt(s) remaining"));

        // Attempt 3 incorrect -> invalidates OTP
        SecurityException ex3 = assertThrows(SecurityException.class, () -> {
            employeeVerificationService.verifyEmployeeCode("anjali.mohan@fillthepockets.demo", wrongCode);
        });
        assertTrue(ex3.getMessage().contains("Maximum incorrect verification attempts exceeded"));

        // Attempt 4 with the correct code -> must be rejected because code was invalidated
        assertThrows(Exception.class, () -> {
            employeeVerificationService.verifyEmployeeCode("anjali.mohan@fillthepockets.demo", correctCode);
        });

        // 2. Expired code test on Rahul Joseph
        EmployeeOtpResponse initResp2 = employeeVerificationService.initiateEmployeeLogin(
                "rahul.joseph@fillthepockets.demo", "Demo@Emp123"
        );
        OtpRecord otp = otpRecordRepository.findTopByEmailOrderByCreatedAtDesc("rahul.joseph@fillthepockets.demo").orElseThrow();
        otp.setExpiresAt(java.time.LocalDateTime.now().minusSeconds(10));
        otpRecordRepository.save(otp);

        IllegalArgumentException expiredEx = assertThrows(IllegalArgumentException.class, () -> {
            employeeVerificationService.verifyEmployeeCode("rahul.joseph@fillthepockets.demo", initResp2.getDemoOtpCode());
        });
        assertTrue(expiredEx.getMessage().contains("expired"), "Must report expired verification code");
    }
}

# Fill the Pockets – Smart Village Road Reporting & Rewards System

**Fill the Pockets** is a full-stack, college project engineered for Gram Panchayat rural civic management. It empowers villagers to photograph road damage (potholes, asphalt breakdown) and report it with GPS coordinates. An integrated deep learning AI vision model automatically inspects the image, verifies road damage, and awards 20 points to the citizen's rewards wallet. Panchayat employees manage and update road repairs without manual verification or reward bottlenecks.

---

## 🌐 Localhost Access Links

| Portal | Local URL | Description |
|---|---|---|
| **Main Website & Homepage** | **[http://localhost:8080](http://localhost:8080)** | Hero, live statistics, how it works, feature breakdown |
| **Villager Login** | **[http://localhost:8080/villager-login.html](http://localhost:8080/villager-login.html)** | Direct citizen login (No OTP required) |
| **Villager Registration** | **[http://localhost:8080/villager-register.html](http://localhost:8080/villager-register.html)** | Citizen registration |
| **Villager Dashboard** | **[http://localhost:8080/villager-dashboard.html](http://localhost:8080/villager-dashboard.html)** | Personal submissions, repair status, points balance |
| **Report a Road Problem** | **[http://localhost:8080/report-problem.html](http://localhost:8080/report-problem.html)** | Image upload, live GPS capture, instant AI verification |
| **Rewards & Wallet** | **[http://localhost:8080/wallet-rewards.html](http://localhost:8080/wallet-rewards.html)** | Points transaction ledger, coupon redemption store |
| **Panchayat Employee Login** | **[http://localhost:8080/employee-login.html](http://localhost:8080/employee-login.html)** | 2FA verification code modal pop-up login |
| **Employee Dashboard** | **[http://localhost:8080/employee-dashboard.html](http://localhost:8080/employee-dashboard.html)** | Road repair queue management, filters, audit updates |

---

## 🏛️ Preconfigured Demo Panchayat Employee Accounts

The database is seeded with 5 demonstration employee accounts:

| Employee ID | Employee Name | Designation | Email Address | Default Password | 2FA Pop-up Code |
|---|---|---|---|---|---|
| `EMP001` | **Arun Kumar** | Executive Officer | `arun.kumar@fillthepockets.demo` | `Demo@Emp123` | Displayed in demo modal |
| `EMP002` | **Anjali Mohan** | Assistant Engineer (Civil) | `anjali.mohan@fillthepockets.demo` | `Demo@Emp123` | Displayed in demo modal |
| `EMP003` | **Rahul Joseph** | Road & Infra Overseer | `rahul.joseph@fillthepockets.demo` | `Demo@Emp123` | Displayed in demo modal |
| `EMP004` | **Meera Thomas** | Panchayat Secretary | `meera.thomas@fillthepockets.demo` | `Demo@Emp123` | Displayed in demo modal |
| `EMP005` | **Nikhil Raj** | Field Works Inspector | `nikhil.raj@fillthepockets.demo` | `Demo@Emp123` | Displayed in demo modal |

> **Note on Demo Mode:** When logging into the Panchayat Employee portal, enter one of the demo emails and `Demo@Emp123`. The system verifies the employee credentials and launches the **verification code pop-up modal**. In college demo mode, the 6-digit code is clearly displayed in a dedicated demo banner and automatically prefilled.

---

## 🛠️ Technology Stack

- **Frontend:** Responsive HTML5, CSS3, Vanilla JavaScript (Forest Green, Cream, White, and Gold theme).
- **Backend:** Java 17 with Spring Boot 3.2.5 (Spring Web, Spring Security, Spring Data JPA, JWT).
- **Database:** MySQL / MariaDB (running on `localhost:3306`, database `fill_the_pockets`).
- **AI Verification:** Microsoft ONNX Runtime (`mobilenetv2-7.onnx`) combined with Computer Vision surface gradient and texture analysis.
- **Duplicate Prevention:** Perceptual Image Difference Hashing (dHash 64-bit) + Haversine GPS proximity (15-meter radius).

---

## 🧪 Integration Test Suite Results

All **9 comprehensive integration tests** have run and passed with **0 errors and 0 failures**:
- `testFiveDemoEmployeesExist` — PASSED
- `testVillagerRegistrationAndLogin` — PASSED
- `testEmployeeTwoFactorFlow` — PASSED
- `testSecurityRoleSeparation` — PASSED
- `testAutomaticAiPhotoVerificationAndPoints` — PASSED (Verified damage awarded +20 pts)
- `testAiRejectionForUnrelatedPhoto` — PASSED (Unrelated photo rejected with 0 pts)
- `testDuplicatePrevention` — PASSED (Duplicate image/location rejected with 0 pts)
- `testEmployeeRepairUpdateWorkflow` — PASSED (Status updated with audit trail notes)
- `testRewardCouponRedemption` — PASSED (Voucher generated, points deducted in MySQL transaction)

---

## 🚀 Run Locally

Requirements: Java 17, Maven, and MySQL or MariaDB. Create the `fill_the_pockets` database, then configure the database connection with the standard Spring environment variables if it differs from the local defaults in `application.properties`.

In PowerShell, set a unique JWT signing key for this run and start the application:

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
$env:APP_JWT_SECRET = [Convert]::ToBase64String($bytes)
$rng.Dispose()
mvn spring-boot:run
```

For a packaged run, build with `mvn package` and then use `java -jar target\fill-the-pockets-1.0.0.jar` in the same PowerShell session. Open [http://localhost:8080](http://localhost:8080) in a browser.

Demo employee accounts are seeded when `app.demo.mode` is enabled (the default for this demo project). Set `APP_DEMO_MODE=false` in a deployed environment to prevent demo employee accounts from being seeded. Configure `APP_JWT_SECRET` with a strong, private value in every deployed environment; do not reuse a development key.

---

## ☁️ Deployment Notes

This repository contains the application source; pushing it to GitHub does not itself host or run the application. For a hosted deployment, provide a MySQL-compatible database and configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_JWT_SECRET`, and `APP_DEMO_MODE=false` in the hosting provider's environment settings. Keep credentials out of source control. Configure a real OTP delivery provider before enabling non-demo employee verification.

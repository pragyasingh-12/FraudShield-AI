# Test plan

Status legend: **Passed (verified)** = executed against the running application in the verification pass (HTTP requests with `curl` plus SQL checks) on the environment listed in README section 25. Earlier "dev run" entries were re-run on the final code unless noted.

| ID | Test | Input | Expected result | Status |
|---|---|---|---|---|
| TC-01 | Valid login | admin@fraudshield.local / Admin@123 | 302 redirect to /dashboard | Passed (verified) |
| TC-02 | Invalid login | valid e-mail, wrong password | HTTP 401, "Invalid e-mail or password." | Passed (verified) |
| TC-03 | Customer cannot sign in | rahul.sharma@example.com / Customer@123 | HTTP 401 | Passed (verified) |
| TC-04 | Page without session | GET /dashboard, no cookie | 302 to /login | Passed (verified) |
| TC-05 | Missing CSRF token | POST /transactions/add without token | HTTP 403 | Passed (verified) |
| TC-06 | Valid normal transaction | Rahul, Rs 2,400, swiggy@icici, 13:15, DEV-RAHUL-A1, Delhi | Saved, status APPROVED, score about 0-10 (LOW), no alert | Passed (verified: score 1) |
| TC-07 | Negative amount | amount = -5 | Form re-shown with "Amount must be greater than zero.", HTTP 400 | Passed (verified) |
| TC-08 | Invalid UPI id | receiver = `notvalid` | "UPI receiver must look like name@bank..." | Passed (verified) |
| TC-09 | Non-numeric amount | amount = `abc` | "Amount must be a valid number." | Passed (verified) |
| TC-10 | Suspicious high-value transaction | Priya, Rs 60,000, 03:20, DEV-UNKNOWN-7Z, Kolkata, quick.cash@paytm | HIGH or CRITICAL, status REVIEW or BLOCKED, alert created, reasons listed | Passed (verified: 78 HIGH, 5 reasons, alert created) |
| TC-11 | Repeated transactions | "Simulate rapid burst", 5 payments | All 5 analysed in parallel, frequency factor grows with each payment | Passed (verified: rule score 23, 23, 37, 35, 35) |
| TC-12 | New device | Known customer, unknown device id | Device factor 70 (+10.5 points) and reason "New device detected" | Passed (dev run, see TC-10) |
| TC-13 | Unusual location | Location different from usual | Location factor 100 (+15 points) and reason shown | Passed (dev run, see TC-10) |
| TC-14 | Risk calculation | Seeded TXN1122-TXN1125 | Level matches the 0-30 / 31-60 / 61-80 / 81-100 bands; final = round(0.75 x rule + 0.25 x ML) | Passed (verified: 78 HIGH, 84/88/93 CRITICAL) |
| TC-15 | ML prediction | Any analysed transaction | `ml_score` present in `risk_analysis`; Analytics page shows READY model | Passed (verified) |
| TC-16 | Dry-run analysis | /analyze with a suspicious payment | Result shown, `COUNT(*)` of transactions unchanged | Passed (verified) |
| TC-17 | Fraud alert creation | Any score above 30 | Row in `fraud_alerts` with reason text and status OPEN | Passed (verified) |
| TC-18 | Alert decision | FALSE_POSITIVE / CONFIRMED_FRAUD | Transaction becomes APPROVED / BLOCKED | Passed (verified) |
| TC-19 | Missing transaction | /risk?id=99999 and /risk?id=abc | HTTP 404 with friendly page | Passed (verified) |
| TC-20 | Pending analysis | Row with status PENDING and no analysis | Page says "Analysis in progress" and refreshes every 2 s | Passed (verified) |
| TC-21 | Database failure | Stop MySQL, open /history, then log in | HTTP 500 friendly page; login shows 503 message; app recovers when MySQL returns | Passed (verified) |
| TC-22 | Search and filters | `?q=TXN1132`, `?level=CRITICAL&min=40000` | Matching rows only | Passed (verified) |
| TC-23 | Role check | Analyst POSTs /users action=add | HTTP 403 | Passed (verified) |
| TC-24 | Add user and device | Admin creates user and device | Rows appear on Users / user detail pages | Passed (verified: HTTP 302, rows created) |
| TC-25 | Lock-out | 5 wrong passwords | "Too many failed attempts" for 5 minutes | Passed (verified) |
| TC-26 | SQL injection attempt | `' OR '1'='1` in search or login | No error, no extra rows (PreparedStatement) | Passed (verified) |
| TC-27 | Weka evaluation | Start application | Log line "Weka model status: READY"; Analytics shows hold-out and 10-fold figures | Passed (dev run, Weka 3.7.11 jar) |
| TC-28 | JDBC atomic commit | Alert insert delayed 4 s by a DB trigger while a HIGH payment is analysed | During the delay: status PENDING, 0 analysis rows, 0 alerts, page shows "Analysis in progress"; after commit all three appear together (REVIEW 78, 1 analysis, 1 alert) | Passed (verified) |
| TC-29 | JDBC rollback | Alert insert forced to fail by a DB trigger | Transaction stays PENDING, no analysis row, no alert, application keeps serving; failure is logged at SEVERE; after restart the pending transaction is analysed | Passed (verified) |
| TC-30 | ML failure fallback | Weka dependency missing (`NoClassDefFoundError`) | Model status FAILED, application still starts, pending transactions are still analysed with the rule engine | Passed (verified after fix, see verification report) |

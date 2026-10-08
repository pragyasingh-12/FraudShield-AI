# Workflow

## Application workflow

Login -> Dashboard -> Transaction entry -> Validation -> Database storage (PENDING) -> Behavioural analysis -> Rule-based detection -> ML analysis -> Risk scoring -> Explainable result -> Fraud alert -> Dashboard update.

| Step | Class / method | What happens |
|---|---|---|
| Login | `LoginServlet` -> `AuthenticationService.login` | Salted hash check, lock-out after 5 failures, new session + CSRF token |
| Dashboard | `DashboardServlet` -> `DashboardService.getStats` | Reads the cached snapshot |
| Entry | `TransactionServlet.doPost` -> `parse` | Converts request text into a `Transaction` |
| Validation | `TransactionService.validate` | Amount range, UPI id pattern, device id pattern, time, customer exists |
| Storage | `TransactionDAO.create` | `INSERT` with status PENDING |
| Async analysis | `FraudDetectionService.analyzeAsync` | `Callable` on the worker pool |
| Behaviour | `BehaviorProfileService.build` | Average, std-dev, max, usual hours (5th-95th percentile), usual location, receivers, types |
| Rules | `RuleBasedAnalyzer` | Six factors, weights 25/20/15/15/15/10 |
| ML | `MLTransactionAnalyzer` -> `MLModelService` | Weka J48 fraud probability |
| Scoring | `HybridRiskAnalyzer` | 0.75 x rule + 0.25 x ML, level from score, explanation text |
| Persist | `FraudDetectionService.persist` | One JDBC transaction: `risk_analysis`, `transactions`, `fraud_alerts` |
| After | `afterPersist` | Approved -> trust device, refresh profile; Blocked -> mark device suspicious; refresh dashboard |

## Fraud detection workflow (step by step)

1. Lock the customer (`userLocks` map) so their transactions are analysed one at a time.
2. Load the customer's APPROVED history older than this transaction and build the profile.
3. Count earlier payments in the 10 minutes, 1 hour and day before the transaction time.
4. Load the customer's known devices and the global set of suspicious devices.
5. Score each factor 0-100 (rules in `RuleBasedAnalyzer`).
6. Ask the Weka model for the fraud probability using seven features.
7. Blend the two scores, clamp to 0-100, classify (0-30 LOW, 31-60 MEDIUM, 61-80 HIGH, 81-100 CRITICAL).
8. Build the explanation from triggered factors (score >= 40), largest contribution first.
9. Map level to status: LOW/MEDIUM -> APPROVED, HIGH -> REVIEW, CRITICAL -> BLOCKED. MEDIUM and above create an alert.
10. Commit everything atomically; roll back on any SQL error.

# Architecture

FraudShield AI is a layered Java web application. Each layer only talks to the layer below it.

| Layer | Package | Responsibility |
|---|---|---|
| Presentation | `webapp/WEB-INF/views`, `css`, `js` | JSP + JSTL/EL pages. Scriptlets are disabled in `web.xml` (`scripting-invalid`). |
| Web | `servlet`, `filter` | Parse and validate request parameters, call one service, set attributes, forward or redirect. `AuthFilter` enforces login and the CSRF token. |
| Service | `service` | Business rules: validation, orchestration, threading, JDBC transaction boundaries. |
| Fraud engine | `analysis` | `FraudDetector` / `RiskAnalyzer` interfaces, `RuleBasedAnalyzer`, `MLTransactionAnalyzer`, `HybridRiskAnalyzer`, `MLModelService` (Weka). |
| Data access | `dao`, `db` | One DAO per table, `DBConnection`. All SQL lives here and uses `PreparedStatement`. |
| Model | `model`, `exception`, `util` | POJOs and enums, custom exceptions, configuration and helpers. |

## Request life-cycle (example: Add transaction)

1. Browser POSTs `/transactions/add` with the CSRF token.
2. `AuthFilter` checks the session and token.
3. `TransactionServlet` parses the parameters into a `Transaction` (no business rules).
4. `TransactionService.submit` validates (`InvalidTransactionException` on failure), stores the row as `PENDING` through `TransactionDAO`, and queues `FraudDetectionService.analyze(id)` on the worker pool.
5. The servlet redirects to `/risk?id=...`; the page refreshes every 2 seconds while the status is `PENDING`.
6. The worker thread takes the customer's lock, builds the behaviour profile, runs `HybridRiskAnalyzer` and writes analysis, status and alert in one JDBC transaction.
7. The next refresh renders the stored explainable `RiskResult`.

## Why these choices

* **Servlets/JSP, not Spring Boot** - the marking rubric emphasises Core Java, JDBC and Servlets.
* **`DriverManager` connections** - simple to explain in a viva. A connection pool (HikariCP / Tomcat JDBC) is the obvious production upgrade (see Future scope).
* **Weka J48** - a decision tree is readable (the learned tree is shown on the Analytics page) and returns class probabilities.
* **Rules are weighted, ML is blended** - the rule score is fully explainable, so ML only adjusts it (25%) and never overrides the explanation.
* **Trusted baseline = APPROVED history only** - blocked or under-review transactions never teach the system that fraud is normal.

See `diagrams.md` for the Mermaid diagrams (architecture, ER, class, flowcharts, threading).

# Presentation content (22 slides)

Keep each slide to a title, 3-5 short bullets and one visual.

| # | Slide | Content | Screenshot / diagram |
|---|---|---|---|
| 1 | Title | FraudShield AI - An Explainable AI-Powered UPI and Digital Payment Fraud Detection System. Course, semester, team, guide | Logo-style title, `01-login.png` |
| 2 | Problem statement | Digital payments are instant and irreversible; fraud shows up as unusual behaviour; a bare "fraud / not fraud" label gives analysts nothing to act on | None, or a one-line example |
| 3 | Motivation | Rising UPI volumes; false alarms annoy customers; missed fraud costs money; analysts need reasons | Simple statistic placeholder from a cited source |
| 4 | Objectives | Detect suspicious payments; score 0-100; explain every flag; store everything in MySQL; run locally in a browser | None |
| 5 | Proposed solution | Rules + customer behaviour + Weka model, combined into one transparent score | Fraud workflow diagram (diagrams.md #5) |
| 6 | System architecture | Browser -> JSP -> Servlet -> Service -> Engine -> DAO -> JDBC -> MySQL, Weka | Architecture diagram (#1) |
| 7 | Technology stack | Java 17, Servlets, JSP/JSTL, JDBC, MySQL 8, Weka, Maven, Tomcat 9 | Logos or table |
| 8 | Database design | 6 tables, PK/FK/constraints | ER diagram (#2), `13-mysql-tables.png` |
| 9 | Fraud detection workflow | 10 steps from PENDING to alert | Flowchart (#5) |
| 10 | OOP implementation | Encapsulation, abstract `TransactionAnalyzer`, interfaces, polymorphism, exceptions | Class diagram (#3) |
| 11 | Collections and generics | `Map`, `Set`, `List`, `DAOOperations<T>`, `PageResult<T>` | Code snippet of `BehaviorProfileService` |
| 12 | Multithreading | Worker pool, per-customer lock, scheduled refresh, burst demo | Threading diagram (#7) |
| 13 | JDBC | `DBConnection`, 6 DAOs, `PreparedStatement`, JDBC transaction | Snippet of `FraudDetectionService.persist` |
| 14 | Servlet and web integration | 10 servlets, filter, listener, JSP/JSTL | Servlet table (README section 16) |
| 15 | ML / Weka | Synthetic 2,000-row dataset, J48, hold-out + 10-fold CV | `09-analytics.png` evaluation table |
| 16 | Explainable risk analysis | Score, level, ticked reasons, factor weights | `04-risk-critical.png` |
| 17 | UI screens | Dashboard, add transaction, history, alerts, analytics | `02-dashboard.png`, `08-alerts.png`, `09-analytics.png` |
| 18 | Demo workflow | Login, normal payment, suspicious payment, burst, alert decision, analytics | Numbered list from README section 31 |
| 19 | Results | Sample results: normal payment scores about 1 (LOW); suspicious payments 78-93 (HIGH/CRITICAL); model figures from the Analytics page, stated as synthetic-data results | Result table |
| 20 | Future scope | Connection pool, real feature store, model retraining, SMS/e-mail alerts, REST API | None |
| 21 | Conclusion | Working, explainable, layered Java system that demonstrates the course concepts | None |
| 22 | Team members | Names, roll numbers, contribution (README section 39) | Table |

# Viva Preparation Guide

**Q1. Why Servlets/JSP and not Spring Boot?**
The rubric emphasises Core Java, JDBC and Servlets. Doing the request handling, session handling and DAO layer by hand shows exactly what a framework would otherwise hide.

**Q2. Explain the architecture.**
Browser -> JSP -> filter -> servlet -> service -> fraud engine -> DAO -> JDBC -> MySQL; Weka sits inside the engine. Servlets are thin; business logic is in services and the engine; SQL is only in DAOs.

**Q3. How is the risk score calculated?**
Six factors scored 0-100 and weighted 25/20/15/15/15/10 give the rule score. The Weka fraud probability is blended 25% / 75% rule. Levels: 0-30 LOW, 31-60 MEDIUM, 61-80 HIGH, 81-100 CRITICAL.

**Q4. How does the system explain a decision?**
Each factor carries a plain-English description built from the same numbers that produced its score. Factors with score >= 40 are listed (ticked) in order of contribution. The `RiskResult` stores score, level, factors, explanation, method and timestamp.

**Q5. Where do you use inheritance and why?**
`TransactionAnalyzer` is an abstract class with a final `analyze()` (validation + counting) and an abstract `doAnalyze()`. `RuleBasedAnalyzer` and `MLTransactionAnalyzer` extend it, so shared behaviour is written once.

**Q6. Where is polymorphism?**
`HybridRiskAnalyzer` holds `FraudDetector` references and calls `analyze()`; which implementation runs is decided at run time. `FraudDetectionService` depends on the `RiskAnalyzer` interface, and the DAOs are used through `DAOOperations<T>`.

**Q7. Abstract class vs interface - why both?**
Interfaces (`FraudDetector`, `RiskAnalyzer`, `DAOOperations`) define contracts; the abstract class adds shared code and enforces the template method for the two analyzers.

**Q8. How is encapsulation shown?**
Private fields and validating setters (`User.setEmail` rejects bad e-mails, `RiskResult.setFinalScore` clamps to 0-100 and derives the level, `getFactors()` returns an unmodifiable list).

**Q9. Which exceptions did you create and why?**
`InvalidTransactionException`, `AuthenticationException`, `FraudAnalysisException`, `DatabaseOperationException`. They make failures specific; `SQLException` is wrapped so upper layers do not depend on JDBC. Servlets turn them into HTTP statuses and friendly pages.

**Q10. Which collections do you use and why?**
`Set` for "seen before" lookups (known devices/locations/receivers), `Map<String,Integer>` for counting (location frequency, level counts), `List` for ordered results, `ConcurrentHashMap` for per-customer locks and failed-login counters, `Optional` for not-found.

**Q11. Where are generics used?**
`DAOOperations<T>` (six implementations), `PageResult<T>`, `Callable<RiskResult>`, `Future<RiskResult>`, `Optional<T>`, typed maps.

**Q12. Why multithreading in a web app?**
Analysis (database reads, rules, ML) should not block the web request, so it runs on a pool of four workers. The burst demo analyses several payments concurrently with `invokeAll`. A scheduled task refreshes dashboard statistics.

**Q13. What is shared and how is it protected?**
A customer's history and velocity counters: per-customer `synchronized` lock from a `ConcurrentHashMap`. The ML classifier: `synchronized` prediction. The dashboard snapshot: `synchronized` refresh + `volatile` publication. Counters: atomics. Suspicious devices: a concurrent set.

**Q14. Why serialise analyses per customer?**
Each result depends on the customer's earlier transactions (velocity, profile). Different customers do not share state, so they run in parallel.

**Q15. How do you avoid a half-saved result or a stuck status?**
Analysis, status and alert are saved in one JDBC transaction (`setAutoCommit(false)`, `commit`, `rollback`). A payment is PENDING until that commit. `analyze()` is idempotent, and pending payments are re-analysed at start-up.

**Q16. How do you prevent SQL injection?**
Every statement that includes user data is a `PreparedStatement` with `?` placeholders; even the dynamic search only appends fixed SQL fragments and binds values.

**Q17. What do `Connection`, `PreparedStatement`, `ResultSet` do and why try-with-resources?**
Connection = session with MySQL; PreparedStatement = pre-compiled parameterised SQL; ResultSet = query rows. try-with-resources closes all three even when exceptions occur, preventing leaks.

**Q18. What is a DAO and why use it?**
A class that hides all SQL for one table behind methods. Services call `transactionDAO.create(t)` without knowing SQL, so the database code is in one place and easy to change.

**Q19. How are passwords stored?**
`salt$SHA-256(salt + password)` with a random salt, compared in constant time. It is not as strong as bcrypt/PBKDF2; that is listed as a limitation.

**Q20. What do the servlet methods do?**
`doGet` shows pages, `doPost` changes data. Each reads parameters, validates, calls a service, sets request/session attributes, and forwards to a JSP or redirects (Post/Redirect/Get after a POST).

**Q21. Forward vs redirect?**
Forward is server-side inside the same request (used to render a JSP and keep request attributes). Redirect sends the browser to a new URL (used after POST to avoid double submission and in the login/logout flow).

**Q22. What do the filter, listener and `web.xml` do?**
`AuthFilter` blocks anonymous users and checks the CSRF token; `AppContextListener` starts the services, ML training and shutdown; `web.xml` configures session cookie flags, JSP rules (no scriptlets) and error pages.

**Q23. Explain the Weka part.**
`data/transactions.csv` (synthetic, 2,000 rows, 7 features + label) is loaded into Weka `Instances`. A J48 decision tree is evaluated with a 70/30 hold-out and 10-fold cross-validation, then trained on all rows. At run time the same 7 features of a live transaction give a fraud probability.

**Q24. Is the accuracy realistic?**
It is accuracy on synthetic data, where fraud labels came from a noisy function of the same features. Accuracy of about 91-92% is close to the 89% you get by always answering "legit", so recall and precision matter more: recall is under 50%. That is why ML is only 25% of the score.

**Q25. Why J48?**
It is readable (the tree is shown on the Analytics page), needs no scaling and gives probabilities. Weka's other classifiers could be swapped in inside `MLModelService`.

**Q26. What is `amount_ratio` and why add it?**
Amount divided by the customer's own average. A raw amount means little without knowing the customer; the ratio lets the tree learn "unusually large for this person".

**Q27. How do you build a behaviour profile?**
From the customer's APPROVED history: mean/std-dev/max amount, usual hours (5th-95th percentile), most frequent location, sets of known locations and receivers, payment-type counts, payments per active day. Fraud-flagged payments are excluded so they cannot poison the baseline.

**Q28. What if the ML model fails?**
`MLTransactionAnalyzer` throws `FraudAnalysisException`; `HybridRiskAnalyzer` catches it and uses the rule score alone, noting "ML unavailable" in the result.

**Q29. What are the main limitations?**
Synthetic data, modest model, no connection pool, simple password hashing, single-node state, no automated unit tests.

**Q30. How would you scale or improve it?**
Connection pooling, persistent model retrained from analyst decisions, message queue for analysis, distributed lock/cache (Redis), more features (geo-distance, merchant category), REST API, unit/integration tests.

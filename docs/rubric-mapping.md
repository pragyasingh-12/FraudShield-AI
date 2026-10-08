# Rubric mapping

Marks: Problem Understanding & Solution Design 8, Core Java 10, Database Integration (JDBC) 8, Servlets & Web Integration 7.

| Academic requirement | Implementation | File / class | Demo evidence |
|---|---|---|---|
| Problem understanding and solution design (8) | UPI/digital-payment fraud is detected by comparing each payment with the customer's own behaviour; results are explained, not just labelled | `README.md` sections 2-12, `docs/architecture.md`, `analysis/*` | Risk page "Why this result" |
| Layered design | JSP -> Servlet -> Service -> Engine -> DAO -> JDBC -> MySQL | packages `servlet`, `service`, `analysis`, `dao`, `db` | Architecture diagram |
| Core Java: classes and encapsulation (10) | Private fields, validating setters, constructors, getters | `model/User`, `Transaction`, `RiskFactor`, `BehaviorProfile` | `User.setEmail()` rejects bad e-mail |
| Inheritance | Abstract `TransactionAnalyzer` with subclasses | `analysis/TransactionAnalyzer`, `RuleBasedAnalyzer`, `MLTransactionAnalyzer` | Template method `analyze()` is `final` |
| Polymorphism | `HybridRiskAnalyzer` holds two `FraudDetector` references; services hold `RiskAnalyzer`; DAOs used through `DAOOperations<T>` | `HybridRiskAnalyzer`, `FraudDetectionService` | Swap detector without touching the caller |
| Abstraction / interfaces | `FraudDetector`, `RiskAnalyzer`, `DAOOperations<T>` | `analysis/`, `dao/` | Class diagram |
| Exception handling | 4 custom checked exceptions; SQLException wrapped in `DatabaseOperationException`; servlets map exceptions to HTTP status and friendly pages | `exception/*`, `BaseServlet.fail`, `LoginServlet` | Negative amount message; stop MySQL and open /history |
| Collections | `List<Transaction>`, `Map<String,Integer>`, `Set<String>`, `HashMap`, `LinkedHashMap`, `ConcurrentHashMap`, `Optional`, `Collections.unmodifiableList`, `Comparator` sort | `BehaviorProfileService`, `DeviceDAO`, `BehaviorProfile`, `FraudDetectionService`, `HybridRiskAnalyzer` | README section 14 |
| Generics | `DAOOperations<T>`, `PageResult<T>`, `Callable<RiskResult>`, `Future<RiskResult>`, `Map<Integer,Object>` | `dao/DAOOperations`, `model/PageResult`, `FraudDetectionService` | History pagination uses `PageResult<Transaction>` |
| Multithreading | Fixed worker pool, `Callable`/`Future`, `invokeAll`, scheduled refresher, start-up task | `AppExecutors`, `FraudDetectionService.analyzeAsync`, `TransactionService.submitBurst`, `DashboardService`, `AppContextListener` | "Simulate rapid burst"; risk page "in progress" |
| Synchronization | Per-customer lock, `synchronized` ML prediction and dashboard refresh, `volatile` snapshots, atomics, concurrent set | `FraudDetectionService.analyze`, `MLModelService`, `DashboardService`, `TransactionAnalyzer` | README section 15 |
| Database classes (DAO) | Six DAOs implement `DAOOperations<T>` | `dao/*` | `ls src/main/java/com/fraudshield/dao` |
| JDBC (8) | `DBConnection`, `PreparedStatement`, `ResultSet`, try-with-resources, `getGeneratedKeys`, batch-free CRUD, dynamic search with placeholders, JDBC transaction (`setAutoCommit(false)`, commit, rollback) | `db/DBConnection`, `dao/*`, `FraudDetectionService.persist` | Add a transaction, then `SELECT` in MySQL |
| CRUD | Users (create/read/update/delete), transactions (create/read/update/delete), alerts (create/read/update status/delete), devices, profiles | `UserDAO`, `TransactionDAO`, `FraudAlertDAO` | Users page, alert decisions, history delete |
| MySQL | 6 tables, PK/FK, UNIQUE, CHECK, ENUM, indexes | `database/schema.sql` | `SHOW CREATE TABLE transactions` |
| Servlets (7) | 10 servlets with `@WebServlet`, `doGet`/`doPost`, sessions, redirects/forwards, status codes | `servlet/*` | README section 16 |
| Web integration | JSP + JSTL/EL (no scriptlets), filter for auth + CSRF, listener for start-up/shutdown, `web.xml` error pages | `filter/AuthFilter`, `AppContextListener`, `web.xml`, `WEB-INF/views` | Open /dashboard while logged out |
| Weka / ML | Real training on `data/transactions.csv`, hold-out + 10-fold CV, probability used in the risk score | `MLModelService`, `MLTransactionAnalyzer`, `util/DatasetGenerator` | Analytics page evaluation table |
| Security basics | `PreparedStatement`, salted hashes, sessions, lock-out, CSRF token, validation, config outside code | `PasswordUtil`, `AuthFilter`, `AppConfig` | POST without token returns 403 |

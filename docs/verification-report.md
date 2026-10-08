# Verification report

Scope: strict final pass on the existing code. Nothing was rewritten; only defects found were fixed.

## A. Build status
| Item | Result |
|---|---|
| `mvn clean package` with the exact `pom.xml` | **Not run** - sandbox cannot reach Maven Central (HTTP 403). |
| Approximation: Maven 3.8.7 `package` with Debian offline plugins (compiler 3.10.1, war 3.3.2) and substituted dependencies | Passed: 64 sources compiled, `target/FraudShieldAI.war` produced (class files major version 61 = Java 17) |
| All sources compile with `--release 17` against real Servlet 4.0.1 API and real Weka 3.8 source | Passed, 0 errors |
| WAR layout | `WEB-INF/web.xml`, 67 class files, 13 JSP + 7 JSP fragments, `WEB-INF/classes/data/transactions.csv`, `application.properties`, CSS/JS |
| pom coordinates (servlet-api 4.0.1, jsp-api 2.3.3, jstl 1.2, mysql-connector-j 8.3.0, weka-stable 3.8.6) | Coordinates reviewed by hand; **not downloaded or resolved** here |

## B. Runtime status (executed)
Login, session, CSRF, roles; Add transaction -> PENDING -> asynchronous analysis -> risk page -> alert -> dashboard/history; dry-run analysis; rapid burst (8 payments, deterministic frequency scores 0/40/70/100...); alert decisions; filters/paging; user and device admin; DB outage and recovery; JDBC atomic commit and rollback; start-up recovery of PENDING rows; Weka training from the CSV (2,000 rows, 217 fraud) and runtime prediction; all 14 page URLs return 200/302 with no JSP errors. Runtime used a MariaDB JDBC driver instead of MySQL Connector/J and Weka built from the 3.8 source snapshot / a 3.7.11 jar.

## C. Rubric status
Verified from the code and by execution: encapsulation, inheritance (`TransactionAnalyzer` -> 2 subclasses; 11 servlets extend `BaseServlet`), abstraction (abstract class with `final analyze()` template method), interfaces (`FraudDetector`, `RiskAnalyzer`, `DAOOperations<T>` x 6), polymorphism (interface-typed detectors), 4 custom exceptions thrown and caught, collections, generics, `ExecutorService`/`Callable`/`Future`/`invokeAll`, `synchronized`/`volatile`/atomics/`ConcurrentHashMap`, per-customer locking, 6 DAOs with CRUD, PreparedStatement-only SQL, JDBC transaction, PK/FK/CHECK/UNIQUE/ENUM constraints and cascades, 11 servlets (`doGet`/`doPost`), sessions, redirect/forward, JSP + JSTL/EL with scriptlets disabled, filter, listener, `web.xml`.

## D. Actual bugs found
1. **ML failure could silently disable start-up analysis.** `MLModelService.train()` caught only `Exception`; a `NoClassDefFoundError` (missing Weka dependency) escaped, the status stayed NOT_TRAINED with no log, and the start-up task died before analysing PENDING transactions.
2. **Failures of asynchronous analyses were invisible.** The exception was stored in a `Future` that nobody reads, so a rolled-back analysis left a PENDING transaction with nothing in the log.

## E. Fixes made
1. `MLModelService.train()` now catches `Exception | LinkageError` and records status FAILED; `AppContextListener` runs ML training and pending-transaction analysis as two independent guarded steps (rules-only analysis continues if ML fails).
2. `FraudDetectionService.analyzeAsync` logs a SEVERE message (with stack trace) before rethrowing into the `Future`.
Both were re-tested after the fix (TC-29, TC-30).

## F. Remaining risks
* The exact `mvn clean package` has not been executed (see A). Check the first build output on your machine.
* MySQL Connector/J 8.3.0 and the packaged `weka-stable` 3.8.6 artifact were not exercised; behaviour was verified with a MariaDB driver and Weka 3.8 source/3.7.11 jar. Weka prints harmless warnings about optional "mtj" jars.
* A failed asynchronous analysis stays PENDING until the next application start (no retry loop).
* MEDIUM-score payments are auto-approved and then become part of the customer's baseline, so a burst of identical large payments scores lower as it goes on (e.g. 59 -> 45 in the test). This is a documented design simplification.
* The ML model is modest (hold-out recall about 37%); it carries 25% of the score.
* Three tiny unused helper methods remain (`TransactionAnalyzer.getAnalysisCount`, `DashboardStats.getComputedAtMillis`, `AlertService.countOpen`); harmless, left in place.
* No JUnit tests; DriverManager connections without a pool; salted SHA-256 instead of bcrypt; single-node state.

# Database design (MySQL 8, database `fraudshield_db`)

Script: `database/schema.sql` (structure + seed users/devices) and `database/sample_data.sql` (130 synthetic transactions).

| Table | Purpose | Key columns | Relationships / constraints |
|---|---|---|---|
| `users` | Console staff (ADMIN, ANALYST) and monitored CUSTOMER accounts | `id` PK, `email` UNIQUE, `password_hash`, `role` | Parent of the other tables |
| `transactions` | Simulated payments | `id` PK, `user_id`, `amount`, `transaction_type`, `receiver`, `transaction_time`, `device_id`, `location`, `status`, `risk_score` | FK `user_id` -> users (CASCADE); CHECK amount > 0; CHECK score 0..100; indexes on (user_id, transaction_time), status, risk_score, device_id |
| `risk_analysis` | One explainable result per transaction | `transaction_id` UNIQUE, six factor scores, `rule_score`, `ml_score`, `final_score`, `risk_level`, `explanation`, `factor_details` | FK -> transactions (CASCADE) |
| `fraud_alerts` | Alert for MEDIUM/HIGH/CRITICAL | `transaction_id` UNIQUE, `risk_level`, `reason`, `alert_status` | FK -> transactions (CASCADE) |
| `devices` | Trusted devices per customer | `device_id`, `device_type`, `first_seen` | FK -> users (CASCADE); UNIQUE (user_id, device_id) |
| `behavior_profiles` | Persisted snapshot of normal behaviour | average / std-dev / max amount, usual hours, usual location, frequency, total | UNIQUE `user_id`, FK -> users (CASCADE) |

## Design notes

* **Transaction reference** `TXN1024` is derived: `"TXN" + (1000 + id)`. No extra column is needed (`Transaction.getReference()` / `idFromReference()`).
* **`risk_score` is NULL until analysed**; `status = PENDING` marks work still in progress.
* **Factor scores are stored in columns** (for SQL analytics such as averages per factor) **and** as text in `factor_details` (to rebuild the exact explanation).
* **Enums** (`ENUM` columns) keep invalid statuses out of the database.
* **Passwords** are stored as `salt$sha256(salt+password)`; the seed script builds the same format with `SHA2()`.
* Ids 3-7 are the seeded customers; their trusted devices are in `devices`.

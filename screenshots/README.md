# Screenshots

Take these screenshots while running the demo (see README section 31) and save them here as PNG files.
The suggested file names match the slides in `docs/presentation-content.md`.

| File name | Page / URL | What must be visible |
|---|---|---|
| 01-login.png | /login | Login form and demo accounts |
| 02-dashboard.png | /dashboard | Six summary cards, risk distribution, recent alerts |
| 03-add-transaction.png | /transactions/add | Form with presets and the rapid-burst control |
| 04-risk-critical.png | /risk?id=... (a CRITICAL transaction) | Score, risk meter, "Why this result" list, factor table |
| 05-risk-low.png | /risk?id=... (a LOW transaction) | Same page for a normal payment |
| 06-dry-run.png | /analyze after submitting | Result with the "Dry run" notice |
| 07-history-filters.png | /history?level=HIGH | Filter form plus filtered table |
| 08-alerts.png | /alerts | Alert list with decision drop-downs |
| 09-analytics.png | /analytics | Hour chart, factor averages, Weka evaluation table |
| 10-users.png / 11-user-detail.png | /users, /users?id=3 | User table, behaviour profile and trusted devices |
| 12-about.png | /about | Weights table and architecture summary |
| 13-mysql-tables.png | MySQL client / Workbench | `SHOW TABLES;` and a `SELECT` on risk_analysis |
| 14-tomcat-console.png | Tomcat log | "Weka model status: READY" and the start-up analysis line |

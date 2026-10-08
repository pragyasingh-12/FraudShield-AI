# FraudShield AI - Diagrams (Mermaid)

All diagrams are plain Mermaid and render directly on GitHub, in VS Code (Markdown Preview Mermaid Support) or at https://mermaid.live.
They describe the code as implemented.

## 1. System architecture

```mermaid
flowchart TD
    B[Browser] --> V[JSP views + CSS/JS]
    V --> F[AuthFilter: session + CSRF check]
    F --> S[Servlets: Login, Dashboard, Transaction, TransactionAnalysis, History, FraudAlert, RiskAnalysis, User, Analytics, About]
    S --> SV[Service layer: Authentication, Transaction, FraudDetection, BehaviorProfile, RiskAnalysis, Alert, User, Dashboard]
    SV --> E[Fraud engine: HybridRiskAnalyzer]
    E --> R[RuleBasedAnalyzer: 6 weighted factors]
    E --> M[MLTransactionAnalyzer]
    M --> W[(Weka J48 model trained from data/transactions.csv)]
    SV --> D[DAO layer: User, Transaction, FraudAlert, RiskAnalysis, Device, BehaviorProfile]
    D --> J[DBConnection - JDBC PreparedStatement]
    J --> DB[(MySQL fraudshield_db)]
```

## 2. ER diagram

```mermaid
erDiagram
    USERS ||--o{ TRANSACTIONS : makes
    USERS ||--o{ DEVICES : owns
    USERS ||--o| BEHAVIOR_PROFILES : has
    TRANSACTIONS ||--o| RISK_ANALYSIS : analysed_by
    TRANSACTIONS ||--o| FRAUD_ALERTS : raises

    USERS {
        int id PK
        string name
        string email UK
        string password_hash
        string phone
        enum role
        timestamp created_at
    }
    TRANSACTIONS {
        int id PK
        int user_id FK
        decimal amount
        enum transaction_type
        string receiver
        datetime transaction_time
        string device_id
        string location
        enum status
        int risk_score
        timestamp created_at
    }
    RISK_ANALYSIS {
        int id PK
        int transaction_id FK
        int amount_score
        int frequency_score
        int time_score
        int device_score
        int location_score
        int behavior_score
        int rule_score
        int ml_score
        int final_score
        enum risk_level
        string analysis_method
        text explanation
        text factor_details
    }
    FRAUD_ALERTS {
        int id PK
        int transaction_id FK
        enum risk_level
        text reason
        enum alert_status
        timestamp created_at
    }
    DEVICES {
        int id PK
        int user_id FK
        string device_id
        string device_type
        timestamp first_seen
    }
    BEHAVIOR_PROFILES {
        int id PK
        int user_id FK
        decimal avg_transaction_amount
        decimal std_dev_amount
        decimal max_amount
        int usual_start_hour
        int usual_end_hour
        string usual_location
        decimal transaction_frequency
        int total_transactions
    }
```

## 3. Class diagram (core classes)

```mermaid
classDiagram
    class FraudDetector {
        <<interface>>
        +analyze(AnalysisContext) DetectionResult
        +getMethodName() String
    }
    class RiskAnalyzer {
        <<interface>>
        +assess(AnalysisContext) RiskResult
    }
    class DAOOperations~T~ {
        <<interface>>
        +create(T) int
        +findById(int) Optional~T~
        +findAll() List~T~
        +update(T) boolean
        +delete(int) boolean
    }
    class TransactionAnalyzer {
        <<abstract>>
        -methodName String
        -analysisCount AtomicLong
        +analyze(AnalysisContext) DetectionResult
        #doAnalyze(AnalysisContext) DetectionResult
    }
    class RuleBasedAnalyzer
    class MLTransactionAnalyzer
    class HybridRiskAnalyzer
    class MLModelService
    FraudDetector <|.. TransactionAnalyzer
    TransactionAnalyzer <|-- RuleBasedAnalyzer
    TransactionAnalyzer <|-- MLTransactionAnalyzer
    RiskAnalyzer <|.. HybridRiskAnalyzer
    HybridRiskAnalyzer o-- FraudDetector : rule + ml
    MLTransactionAnalyzer --> MLModelService

    class UserDAO
    class TransactionDAO
    class FraudAlertDAO
    class RiskAnalysisDAO
    class DeviceDAO
    class BehaviorProfileDAO
    DAOOperations~T~ <|.. UserDAO
    DAOOperations~T~ <|.. TransactionDAO
    DAOOperations~T~ <|.. FraudAlertDAO
    DAOOperations~T~ <|.. RiskAnalysisDAO
    DAOOperations~T~ <|.. DeviceDAO
    DAOOperations~T~ <|.. BehaviorProfileDAO

    class Transaction
    class User
    class RiskResult
    class RiskFactor
    class BehaviorProfile
    class FraudAlert
    class Device
    RiskResult "1" o-- "*" RiskFactor
    Transaction --> User : userId
    FraudAlert --> Transaction : transactionId

    class FraudDetectionService {
        +analyzeAsync(int) Future~RiskResult~
        +analyze(int) RiskResult
        +simulate(Transaction) RiskResult
        +resolveAlert(int, AlertStatus)
    }
    FraudDetectionService --> RiskAnalyzer
    FraudDetectionService --> TransactionDAO
    FraudDetectionService --> RiskAnalysisDAO
    FraudDetectionService --> FraudAlertDAO
    FraudDetectionService --> BehaviorProfileService
```

## 4. Application flowchart

```mermaid
flowchart TD
    A([Start]) --> B[Open /login]
    B --> C{Valid credentials?}
    C -- No --> B
    C -- Yes --> D[Dashboard]
    D --> E[Add transaction]
    E --> F{Input valid?}
    F -- No --> E
    F -- Yes --> G[(Store transaction as PENDING)]
    G --> H[Queue analysis on worker thread]
    H --> I[Risk page shows 'in progress' and refreshes]
    H --> J[Fraud detection workflow]
    J --> K[(Save analysis + status + alert in one DB transaction)]
    K --> L[Dashboard snapshot refreshed]
    L --> M[Risk page shows score, level and reasons]
    M --> N[Alerts: analyst decision]
    N --> O([Logout])
```

## 5. Fraud detection workflow

```mermaid
flowchart TD
    T[Transaction PENDING] --> L{{Acquire lock for this customer}}
    L --> P[Build behaviour profile from APPROVED history]
    P --> C[Count payments in last 10 min / 1 hour / today]
    C --> D[Load known devices and suspicious devices]
    D --> R[Rule engine: amount 25, frequency 20, time 15, device 15, location 15, behaviour 10]
    D --> ML[Weka J48: 7 features -> fraud probability]
    R --> H[Hybrid score = 0.75 x rule + 0.25 x ML]
    ML --> H
    H --> LV{Score}
    LV -- 0-30 --> LOW[LOW: APPROVED, no alert]
    LV -- 31-60 --> MED[MEDIUM: APPROVED + alert]
    LV -- 61-80 --> HIGH[HIGH: REVIEW + alert]
    LV -- 81-100 --> CRIT[CRITICAL: BLOCKED + alert]
    LOW --> S[(Commit analysis, status, alert)]
    MED --> S
    HIGH --> S
    CRIT --> S
    S --> U[Trust device + refresh profile if APPROVED; mark device suspicious if BLOCKED]
```

## 6. Database architecture

```mermaid
flowchart LR
    subgraph Java
      SV[Services] --> DAO[6 DAO classes]
      DAO --> DBC[DBConnection: DriverManager + AppConfig]
    end
    DBC -- JDBC / PreparedStatement --> MY[(MySQL 8: fraudshield_db)]
    subgraph MySQL
      U[users] --> TX[transactions]
      U --> DV[devices]
      U --> BP[behavior_profiles]
      TX --> RA[risk_analysis]
      TX --> FA[fraud_alerts]
    end
    MY --- U
```

## 7. Threading architecture

```mermaid
flowchart TD
    REQ[Tomcat request threads] -->|submit: store PENDING, queue Callable| POOL[analysis-worker pool: 4 threads]
    REQ -->|burst: invokeAll Callables, wait| POOL
    LST[Context listener at start-up] -->|one background task| POOL
    POOL --> LOCK{{Per-customer lock: synchronized on ConcurrentHashMap value}}
    LOCK --> AN[analyze: profile, rules, ML, persist]
    AN --> ML[MLModelService.predict: synchronized]
    AN --> DASH[DashboardService.refresh: synchronized, publishes volatile snapshot]
    SCH[analytics-refresher: ScheduledExecutorService every 30 s] --> DASH
    AN --> SUS[(Concurrent set of suspicious devices)]
    AN --> CNT[(AtomicInteger counters)]
```

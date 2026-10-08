-- =====================================================================
-- FraudShield AI - database schema (MySQL 8.x)
-- Run:  mysql -u root -p < database/schema.sql
-- then: mysql -u root -p fraudshield_db < database/sample_data.sql
-- =====================================================================
DROP DATABASE IF EXISTS fraudshield_db;
CREATE DATABASE fraudshield_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fraudshield_db;

-- ---------------------------------------------------------------- users
-- ADMIN / ANALYST can sign in to the console; CUSTOMER rows are the monitored account holders.
CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,              -- salt$sha256(salt+password)
    phone         VARCHAR(20),
    role          ENUM('ADMIN','ANALYST','CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- --------------------------------------------------------- transactions
CREATE TABLE transactions (
    id               INT AUTO_INCREMENT PRIMARY KEY,   -- public reference = 'TXN' + (1000 + id)
    user_id          INT NOT NULL,
    amount           DECIMAL(12,2) NOT NULL,
    transaction_type ENUM('UPI','CARD','NETBANKING','WALLET') NOT NULL DEFAULT 'UPI',
    receiver         VARCHAR(100) NOT NULL,
    transaction_time DATETIME NOT NULL,
    device_id        VARCHAR(64) NOT NULL,
    location         VARCHAR(60) NOT NULL,
    status           ENUM('PENDING','APPROVED','REVIEW','BLOCKED') NOT NULL DEFAULT 'PENDING',
    risk_score       INT NULL,                         -- NULL until analysed
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_score_range CHECK (risk_score IS NULL OR (risk_score BETWEEN 0 AND 100)),
    INDEX idx_txn_user_time (user_id, transaction_time),
    INDEX idx_txn_status (status),
    INDEX idx_txn_score (risk_score),
    INDEX idx_txn_device (device_id)
) ENGINE=InnoDB;

-- --------------------------------------------------------- risk_analysis
CREATE TABLE risk_analysis (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  INT NOT NULL UNIQUE,               -- one stored analysis per transaction
    amount_score    INT NOT NULL,
    frequency_score INT NOT NULL,
    time_score      INT NOT NULL,
    device_score    INT NOT NULL,
    location_score  INT NOT NULL,
    behavior_score  INT NOT NULL,
    rule_score      INT NOT NULL,                      -- weighted rule/behaviour score 0-100
    ml_score        INT NULL,                          -- Weka fraud probability x100 (NULL if ML unavailable)
    final_score     INT NOT NULL,
    risk_level      ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
    analysis_method VARCHAR(255) NOT NULL,
    explanation     TEXT NOT NULL,
    factor_details  TEXT NULL,                         -- one line per factor: CODE|name|score|weight|description
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_txn FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    INDEX idx_risk_level (risk_level)
) ENGINE=InnoDB;

-- ---------------------------------------------------------- fraud_alerts
CREATE TABLE fraud_alerts (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT NOT NULL UNIQUE,
    risk_level     ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
    reason         TEXT NOT NULL,
    alert_status   ENUM('OPEN','INVESTIGATING','CONFIRMED_FRAUD','FALSE_POSITIVE') NOT NULL DEFAULT 'OPEN',
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_txn FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    INDEX idx_alert_status (alert_status),
    INDEX idx_alert_level (risk_level)
) ENGINE=InnoDB;

-- --------------------------------------------------------------- devices
CREATE TABLE devices (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    device_id   VARCHAR(64) NOT NULL,
    device_type VARCHAR(30) NOT NULL DEFAULT 'MOBILE',
    first_seen  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_device UNIQUE (user_id, device_id)
) ENGINE=InnoDB;

-- ------------------------------------------------------ behavior_profiles
CREATE TABLE behavior_profiles (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    user_id                INT NOT NULL UNIQUE,
    avg_transaction_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    std_dev_amount         DECIMAL(12,2) NOT NULL DEFAULT 0,
    max_amount             DECIMAL(12,2) NOT NULL DEFAULT 0,
    usual_start_hour       TINYINT NOT NULL DEFAULT 8,
    usual_end_hour         TINYINT NOT NULL DEFAULT 22,
    usual_location         VARCHAR(60) NOT NULL DEFAULT '',
    transaction_frequency  DECIMAL(6,2) NOT NULL DEFAULT 0,   -- average transactions per active day
    total_transactions     INT NOT NULL DEFAULT 0,
    updated_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------- seed users
-- Password hash format: salt$SHA2(salt + password, 256)   (same as util.PasswordUtil)
INSERT INTO users (name, email, password_hash, phone, role) VALUES
('Admin User',   'admin@fraudshield.local',   CONCAT('a1d0c6e8','$',SHA2(CONCAT('a1d0c6e8','Admin@123'),256)),    '9000000001', 'ADMIN'),
('Fraud Analyst','analyst@fraudshield.local', CONCAT('b2e1d7f9','$',SHA2(CONCAT('b2e1d7f9','Analyst@123'),256)),  '9000000002', 'ANALYST'),
('Rahul Sharma', 'rahul.sharma@example.com',  CONCAT('c3f2e8a0','$',SHA2(CONCAT('c3f2e8a0','Customer@123'),256)), '9810000003', 'CUSTOMER'),
('Priya Verma',  'priya.verma@example.com',   CONCAT('d4a3f9b1','$',SHA2(CONCAT('d4a3f9b1','Customer@123'),256)), '9820000004', 'CUSTOMER'),
('Amit Patel',   'amit.patel@example.com',    CONCAT('e5b4a0c2','$',SHA2(CONCAT('e5b4a0c2','Customer@123'),256)), '9830000005', 'CUSTOMER'),
('Sneha Iyer',   'sneha.iyer@example.com',    CONCAT('f6c5b1d3','$',SHA2(CONCAT('f6c5b1d3','Customer@123'),256)), '9840000006', 'CUSTOMER'),
('Rohan Gupta',  'rohan.gupta@example.com',   CONCAT('07d6c2e4','$',SHA2(CONCAT('07d6c2e4','Customer@123'),256)), '9850000007', 'CUSTOMER');

-- trusted devices of the seeded customers (user ids 3..7)
INSERT INTO devices (user_id, device_id, device_type, first_seen) VALUES
(3, 'DEV-RAHUL-A1',  'MOBILE',  DATE_SUB(NOW(), INTERVAL 90 DAY)),
(3, 'DEV-RAHUL-LAP', 'BROWSER', DATE_SUB(NOW(), INTERVAL 60 DAY)),
(4, 'DEV-PRIYA-P1',  'MOBILE',  DATE_SUB(NOW(), INTERVAL 80 DAY)),
(5, 'DEV-AMIT-M1',   'MOBILE',  DATE_SUB(NOW(), INTERVAL 75 DAY)),
(6, 'DEV-SNEHA-S1',  'MOBILE',  DATE_SUB(NOW(), INTERVAL 70 DAY)),
(7, 'DEV-ROHAN-R1',  'MOBILE',  DATE_SUB(NOW(), INTERVAL 65 DAY));

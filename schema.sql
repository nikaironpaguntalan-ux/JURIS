
CREATE DATABASE IF NOT EXISTS Juris
    CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE Juris;

CREATE TABLE IF NOT EXISTS users (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(60)  NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,           
    role      ENUM('Admin','Staff','Prosecutor') NOT NULL DEFAULT 'Staff',
    fullName  VARCHAR(120) NOT NULL,
    isActive  ENUM('Active','Inactive') NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO users (username, password, role, fullName, isActive)
VALUES ('admin', 'admin123', 'Admin', 'System Administrator', 'Active');

CREATE TABLE IF NOT EXISTS cases (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    caseID      VARCHAR(30)  NOT NULL UNIQUE,  
    caseType    ENUM('Criminal','Civil','Violations','Administrative','Others') NOT NULL,
    caseNature  VARCHAR(150) NOT NULL,
    caseStatus  ENUM('Active','Resolved','Dismissed') NOT NULL DEFAULT 'Active',
    accused     VARCHAR(150) NOT NULL,
    complainant VARCHAR(150) NOT NULL,
    prosecutor  VARCHAR(120) DEFAULT NULL,
    judge       VARCHAR(120) DEFAULT NULL,
    filedDate   DATE         NOT NULL,
    hearingDate DATE         DEFAULT NULL,
    witness     TEXT         DEFAULT NULL,
    evidence    TEXT         DEFAULT NULL,
    branch      VARCHAR(80)  DEFAULT NULL,
    verdict     VARCHAR(80)  DEFAULT NULL,
    caseDesc    TEXT         NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS schedules (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    caseID    VARCHAR(30)  NOT NULL,
    datetime  DATETIME     NOT NULL,          
    ReSched   DATETIME     DEFAULT NULL,       
    reason    TEXT         DEFAULT NULL,     
    hsID      VARCHAR(60)  DEFAULT NULL,    

    CONSTRAINT fk_sched_case FOREIGN KEY (caseID)
        REFERENCES cases(caseID) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(60)  NOT NULL,
    action    VARCHAR(60)  NOT NULL,
    details   TEXT         DEFAULT NULL,
    log_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

DROP INDEX IF EXISTS idx_cases_status     ON cases;
DROP INDEX IF EXISTS idx_cases_prosecutor ON cases;
DROP INDEX IF EXISTS idx_cases_filedDate  ON cases;
DROP INDEX IF EXISTS idx_sched_datetime   ON schedules;

CREATE INDEX idx_cases_status     ON cases(caseStatus);
CREATE INDEX idx_cases_prosecutor ON cases(prosecutor);
CREATE INDEX idx_cases_filedDate  ON cases(filedDate);
CREATE INDEX idx_sched_datetime   ON schedules(datetime);

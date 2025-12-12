-- Database Schema for Manager Application (MySQL Version)
-- Generated from Manager/src/main/java/database/DatabaseManager.java

-- Table: computer
-- Stores information about monitored computers (Agents)
CREATE TABLE IF NOT EXISTS computer (
    mac_address VARCHAR(50) PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL,
    ip_address VARCHAR(50) NOT NULL,
    os VARCHAR(255),
    architecture VARCHAR(50),
    manufacturer VARCHAR(255),
    model VARCHAR(255),
    serial_number VARCHAR(255),
    cpu_name VARCHAR(255),
    cpu_vendor VARCHAR(255),
    physical_cores INT,
    logical_cores INT,
    cpu_max_freq BIGINT,
    UNIQUE (hostname)
);

-- Table: session
-- Stores monitoring sessions (snapshots of system state) for each computer
CREATE TABLE IF NOT EXISTS session (
    id INT PRIMARY KEY AUTO_INCREMENT,
    mac_address VARCHAR(50),
    cpu_usage DOUBLE,
    total_ram BIGINT,
    ram_usage BIGINT,
    timestamp BIGINT,
    FOREIGN KEY (mac_address) REFERENCES computer(mac_address)
);

-- Table: process
-- Stores process details for a specific session
CREATE TABLE IF NOT EXISTS process (
    id INT PRIMARY KEY AUTO_INCREMENT,
    pid INT,
    session_id INT,
    name VARCHAR(255),
    cpu_usage DOUBLE,
    ram_usage BIGINT,
    FOREIGN KEY (session_id) REFERENCES session(id),
    UNIQUE KEY unique_process_session (pid, session_id)
);

-- Table: users
-- Stores user credentials for authentication
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    UNIQUE (username)
);

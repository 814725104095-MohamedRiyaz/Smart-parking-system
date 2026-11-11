CREATE DATABASE IF NOT EXISTS smart_parking
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE smart_parking;

CREATE TABLE IF NOT EXISTS admin (
    admin_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL DEFAULT '',
    email VARCHAR(254) NULL,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_salt VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_admin_email (email)
);

CREATE TABLE IF NOT EXISTS parking_slots (
    slot_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slot_number VARCHAR(20) NOT NULL UNIQUE,
    status ENUM('AVAILABLE', 'OCCUPIED') NOT NULL DEFAULT 'AVAILABLE'
);

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_number VARCHAR(20) NOT NULL UNIQUE,
    owner_name VARCHAR(120) NOT NULL,
    type ENUM('CAR', 'BIKE', 'OTHER') NOT NULL
);

CREATE TABLE IF NOT EXISTS parking_records (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME NULL,
    fee DECIMAL(10, 2) NULL,
    CONSTRAINT fk_record_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
    CONSTRAINT fk_record_slot FOREIGN KEY (slot_id) REFERENCES parking_slots(slot_id),
    INDEX idx_record_exit_time (exit_time),
    INDEX idx_record_entry_time (entry_time)
);

CREATE TABLE IF NOT EXISTS waiting_queue (
    queue_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_number VARCHAR(20) NOT NULL UNIQUE,
    owner_name VARCHAR(120) NOT NULL,
    vehicle_type ENUM('CAR', 'BIKE', 'OTHER') NOT NULL,
    queued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO parking_slots (slot_number) VALUES
    ('A-01'), ('A-02'), ('A-03'), ('A-04'), ('A-05'),
    ('B-01'), ('B-02'), ('B-03'), ('B-04'), ('B-05'),
    ('C-01'), ('C-02'), ('C-03'), ('C-04'), ('C-05');

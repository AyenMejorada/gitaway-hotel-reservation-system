-- ============================================================
-- NOTE: This file is the single source of truth for the database. (hotel.sql is deprecated and has been renamed to hotel_DEPRECATED_DO_NOT_USE.sql.bak).
-- Hotel Reservation System - Database Schema
-- ============================================================
-- Run this script in MySQL before starting the application:
--   mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS hotel_reservation_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE hotel_reservation_db;

-- ------------------------------------------------------------
-- Users table (login accounts for Admin and Customer roles)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    role           ENUM('ADMIN', 'CUSTOMER') NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(100),
    is_active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- Rooms table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
    room_id        INT AUTO_INCREMENT PRIMARY KEY,
    room_number    VARCHAR(10)  NOT NULL UNIQUE,
    room_type      ENUM('SINGLE', 'DOUBLE', 'DELUXE', 'SUITE') NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    status         ENUM('AVAILABLE', 'RESERVED', 'OCCUPIED', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    capacity       INT NOT NULL DEFAULT 1,
    description    VARCHAR(255),
    is_deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- Guests table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS guests (
    guest_id       INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NULL,
    first_name     VARCHAR(50) NOT NULL,
    last_name      VARCHAR(50) NOT NULL,
    email          VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    address        VARCHAR(255),
    id_number      VARCHAR(50),
    is_deleted     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_guest_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- Reservations table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id   INT AUTO_INCREMENT PRIMARY KEY,
    guest_id         INT NOT NULL,
    room_type        ENUM('SINGLE', 'DOUBLE', 'DELUXE', 'SUITE') NOT NULL,
    room_id          INT NULL,
    check_in_date    DATE NOT NULL,
    check_out_date   DATE NOT NULL,
    num_guests       INT NOT NULL DEFAULT 1,
    status           ENUM('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    total_amount     DECIMAL(10,2) NOT NULL DEFAULT 0,
    notes            VARCHAR(255),
    is_deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_res_guest FOREIGN KEY (guest_id) REFERENCES guests(guest_id),
    CONSTRAINT fk_res_room  FOREIGN KEY (room_id)  REFERENCES rooms(room_id)
);

-- ------------------------------------------------------------
-- Billing table
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS billing (
    bill_id          INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id    INT NOT NULL,
    room_charges      DECIMAL(10,2) NOT NULL DEFAULT 0,
    additional_charges DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount          DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax               DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount      DECIMAL(10,2) NOT NULL DEFAULT 0,
    payment_status    ENUM('UNPAID', 'PARTIAL', 'PAID') NOT NULL DEFAULT 'UNPAID',
    payment_method    ENUM('CASH', 'CARD', 'GCASH', 'BANK_TRANSFER', 'NONE') NOT NULL DEFAULT 'NONE',
    is_deleted        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bill_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id)
);

-- ------------------------------------------------------------
-- Seed data
-- ------------------------------------------------------------

-- Default admin account -> username: admin / password: admin123
INSERT INTO users (username, password, role, full_name, email) VALUES
('admin', 'admin123', 'ADMIN', 'System Administrator', 'admin@hotel.com')
ON DUPLICATE KEY UPDATE username = username;

-- Default customer account -> username: customer / password: customer123
INSERT INTO users (username, password, role, full_name, email) VALUES
('customer', 'customer123', 'CUSTOMER', 'Juan Dela Cruz', 'juan@example.com')
ON DUPLICATE KEY UPDATE username = username;

-- Default guest account -> username: guest / password: guest123
INSERT INTO users (username, password, role, full_name, email) VALUES
('guest', 'guest123', 'CUSTOMER', 'Guest User', 'guest@example.com')
ON DUPLICATE KEY UPDATE username = username;


-- Sample rooms (20 rooms per type: 101-120, 201-220, 301-320, 401-420)
INSERT INTO rooms (room_number, room_type, price_per_night, status, capacity, description) VALUES
('101', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room with city view'),
('102', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room with garden view'),
('103', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('104', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('105', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('106', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('107', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('108', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('109', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('110', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('111', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('112', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('113', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('114', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('115', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('116', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('117', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('118', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('119', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('120', 'SINGLE', 1500.00, 'AVAILABLE', 1, 'Cozy single room'),
('201', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('202', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Double room with balcony'),
('203', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('204', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('205', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('206', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('207', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('208', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('209', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('210', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('211', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('212', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('213', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('214', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('215', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('216', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('217', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('218', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('219', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('220', 'DOUBLE', 2500.00, 'AVAILABLE', 2, 'Spacious double room'),
('301', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room with king bed'),
('302', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('303', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('304', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('305', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('306', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('307', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('308', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('309', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('310', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('311', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('312', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('313', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('314', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('315', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('316', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('317', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('318', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('319', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('320', 'DELUXE', 4000.00, 'AVAILABLE', 3, 'Deluxe room'),
('401', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite with living area'),
('402', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('403', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('404', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('405', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('406', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('407', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('408', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('409', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('410', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('411', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('412', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('413', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('414', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('415', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('416', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('417', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('418', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('419', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite'),
('420', 'SUITE', 7000.00, 'AVAILABLE', 4, 'Executive suite')
ON DUPLICATE KEY UPDATE room_number = room_number;

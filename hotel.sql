-- ============================================================
-- hotel_db - Hotel Reservation System
-- ============================================================

CREATE DATABASE IF NOT EXISTS hotel_db;
USE hotel_db;

-- ============================================================
-- USERS TABLE (login accounts)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    role          TINYINT      NOT NULL DEFAULT 0,  -- 0 = customer, 1 = admin
    failed_attempts TINYINT    NOT NULL DEFAULT 0,
    is_locked     TINYINT      NOT NULL DEFAULT 0,  -- 0 = unlocked, 1 = locked
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- GUESTS TABLE (customer profile)
-- ============================================================
CREATE TABLE IF NOT EXISTS guests (
    guest_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT          NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone         VARCHAR(20)  NOT NULL,
    address       TEXT,
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================================
-- ROOMS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS rooms (
    room_id       INT AUTO_INCREMENT PRIMARY KEY,
    room_number   VARCHAR(10)  NOT NULL UNIQUE,
    room_type     VARCHAR(50)  NOT NULL,  -- Standard, Deluxe, Suite
    price_per_night DECIMAL(10,2) NOT NULL,
    capacity      INT          NOT NULL DEFAULT 1,
    status        VARCHAR(20)  NOT NULL DEFAULT 'Available', -- Available, Occupied, Maintenance
    description   TEXT,
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- RESERVATIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id  INT AUTO_INCREMENT PRIMARY KEY,
    guest_id        INT          NOT NULL,
    room_id         INT          NOT NULL,
    check_in_date   DATE         NOT NULL,
    check_out_date  DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'Pending', -- Pending, Confirmed, Checked-In, Checked-Out, Cancelled
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guest_id)  REFERENCES guests(guest_id)  ON DELETE CASCADE,
    FOREIGN KEY (room_id)   REFERENCES rooms(room_id)    ON DELETE CASCADE
);

-- ============================================================
-- BILLING TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS billing (
    bill_id         INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id  INT            NOT NULL,
    total_amount    DECIMAL(10,2)  NOT NULL,
    payment_status  VARCHAR(20)    NOT NULL DEFAULT 'Unpaid', -- Unpaid, Paid
    payment_method  VARCHAR(50),   -- Cash, Card, Online
    payment_date    DATETIME,
    created_at      DATETIME       DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE
);

-- ============================================================
-- SEED DATA - Default admin account
-- ============================================================
INSERT INTO users (username, password, role) VALUES
('admin', 'admin123', 1),
('customer1', 'cust123', 0);

INSERT INTO guests (user_id, full_name, email, phone, address) VALUES
(2, 'Juan Dela Cruz', 'juan@email.com', '09171234567', 'Manila, Philippines');

INSERT INTO rooms (room_number, room_type, price_per_night, capacity, status, description) VALUES
('101', 'Standard', 1500.00, 1, 'Available', 'Single bed, air-conditioned room'),
('102', 'Standard', 1500.00, 2, 'Available', 'Twin bed, air-conditioned room'),
('201', 'Deluxe',   2500.00, 2, 'Available', 'Queen bed with city view'),
('202', 'Deluxe',   2500.00, 2, 'Available', 'King bed with garden view'),
('301', 'Suite',    5000.00, 4, 'Available', 'Living area, jacuzzi, ocean view');
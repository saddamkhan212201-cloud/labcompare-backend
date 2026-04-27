-- LabCompare MySQL Schema
-- Run this on your AWS RDS instance before first deploy with mysql profile

CREATE DATABASE IF NOT EXISTS labcompare CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE labcompare;

-- Tables are auto-created by Hibernate (spring.jpa.hibernate.ddl-auto=update)
-- This file is for manual reference or fresh installs

-- To manually create:
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('SUPERADMIN','ADMIN','USER') NOT NULL DEFAULT 'USER',
    admin_lab_id BIGINT NULL,
    FOREIGN KEY (admin_lab_id) REFERENCES labs(id) ON DELETE SET NULL
);


    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50),
    rating DECIMAL(3,1),
    accreditation VARCHAR(255),
    home_collection BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS tests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS lab_test_prices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lab_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    price DOUBLE NOT NULL,
    discount_percent DOUBLE DEFAULT 0,
    report_duration VARCHAR(100),
    UNIQUE KEY uq_lab_test (lab_id, test_id),
    FOREIGN KEY (lab_id) REFERENCES labs(id) ON DELETE CASCADE,
    FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_ref VARCHAR(50) NOT NULL UNIQUE,
    patient_name VARCHAR(255) NOT NULL,
    patient_age INT,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    lab_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    test_price DOUBLE,
    collection_fee DOUBLE,
    total_amount DOUBLE,
    collection_type VARCHAR(20),
    collection_address TEXT,
    appointment_date DATE NOT NULL,
    appointment_slot VARCHAR(50) NOT NULL,
    payment_method VARCHAR(20),
    status VARCHAR(20) DEFAULT 'CONFIRMED',
    created_at DATETIME,
    FOREIGN KEY (lab_id) REFERENCES labs(id),
    FOREIGN KEY (test_id) REFERENCES tests(id)
);

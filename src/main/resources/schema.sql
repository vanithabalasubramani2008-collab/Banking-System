-- =====================================================================
-- SecureBank - Banking Management System Database Schema
-- Compatible with MySQL 8.0+
-- =====================================================================

CREATE DATABASE IF NOT EXISTS securebank_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE securebank_db;

-- 1. Users Table
-- Stores user credentials, hashed passwords, hashed PINs, and security lockout states
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    pin_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(15),
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_CUSTOMER',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    is_account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    lock_time DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_username (username),
    INDEX idx_user_email (email)
) ENGINE=InnoDB;

-- 2. Base Accounts Table (Inheritance Type: JOINED)
-- Holds base fields for all accounts: account number, owner user_id, balance, and account type
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    account_holder_name VARCHAR(100) NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    account_type VARCHAR(31) NOT NULL,
    version BIGINT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_acc_number (account_number),
    INDEX idx_acc_user (user_id)
) ENGINE=InnoDB;

-- 3. Savings Accounts Sub-Table (Joined Table)
-- Extends accounts table with minimum balance requirement (₹1,000) and interest rate (4%)
CREATE TABLE IF NOT EXISTS savings_accounts (
    id BIGINT PRIMARY KEY,
    minimum_balance DECIMAL(15, 2) NOT NULL DEFAULT 1000.00,
    interest_rate DECIMAL(5, 4) NOT NULL DEFAULT 0.0400,
    CONSTRAINT fk_savings_account FOREIGN KEY (id) REFERENCES accounts(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 4. Current Accounts Sub-Table (Joined Table)
-- Extends accounts table with overdraft facility (up to ₹5,000)
CREATE TABLE IF NOT EXISTS current_accounts (
    id BIGINT PRIMARY KEY,
    overdraft_limit DECIMAL(15, 2) NOT NULL DEFAULT 5000.00,
    CONSTRAINT fk_current_account FOREIGN KEY (id) REFERENCES accounts(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 5. Transactions Table
-- Immutable, persistent ledger for deposits, withdrawals, transfers, and interest credits
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    description VARCHAR(255),
    transfer_reference VARCHAR(50) NULL,
    counterparty_account_number VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_tx_account_created (account_id, created_at DESC),
    INDEX idx_tx_reference (transfer_reference)
) ENGINE=InnoDB;

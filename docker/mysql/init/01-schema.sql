-- Hospital Management System - MySQL schema.
--
-- The official MySQL image runs every .sql file in /docker-entrypoint-initdb.d
-- once, in filename order, on first startup of an empty data volume. To re-run:
--   docker compose down -v && docker compose up -d
--
-- Schema changes after the first release go in db/migrations/ as numbered files;
-- see the "Database migrations" section of the README.

CREATE DATABASE IF NOT EXISTS hospital_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE hospital_db;

-- ---------------------------------------------------------------------------
-- Staff accounts. Roles drive both menu visibility and server-side access
-- control; see com.hms.model.Role.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(120) NOT NULL,
  role          VARCHAR(20)  NOT NULL,
  doctor_id     INT          NULL,
  active        BOOLEAN      NOT NULL DEFAULT TRUE,
  last_login_at DATETIME     NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_users_username (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS patients (
  id                 INT AUTO_INCREMENT PRIMARY KEY,
  full_name          VARCHAR(120) NOT NULL,
  gender             VARCHAR(10)  NOT NULL,
  date_of_birth      DATE         NULL,
  phone              VARCHAR(20)  NULL,
  email              VARCHAR(120) NULL,
  address            VARCHAR(255) NULL,
  blood_group        VARCHAR(5)   NULL,
  insurance_provider VARCHAR(120) NULL,
  insurance_number   VARCHAR(60)  NULL,
  allergies          VARCHAR(255) NULL,
  medical_history    TEXT         NULL,
  created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_patients_name (full_name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS doctors (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  full_name        VARCHAR(120)  NOT NULL,
  specialization   VARCHAR(80)   NOT NULL,
  phone            VARCHAR(20)   NULL,
  email            VARCHAR(120)  NULL,
  consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  available        BOOLEAN       NOT NULL DEFAULT TRUE,
  -- Weekly schedule: comma-separated day codes plus the daily consulting window.
  available_days   VARCHAR(40)   NULL,
  available_from   TIME          NULL,
  available_to     TIME          NULL,
  room_number      VARCHAR(20)   NULL,
  created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_doctors_specialization (specialization)
) ENGINE=InnoDB;

ALTER TABLE users
  ADD CONSTRAINT fk_users_doctor FOREIGN KEY (doctor_id)
    REFERENCES doctors (id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS appointments (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  patient_id       INT          NOT NULL,
  doctor_id        INT          NOT NULL,
  appointment_time DATETIME     NOT NULL,
  reason           VARCHAR(255) NULL,
  status           VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- Deleting a patient or doctor removes their appointments; the UI warns first.
  CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id)
    REFERENCES patients (id) ON DELETE CASCADE,
  CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id)
    REFERENCES doctors (id) ON DELETE CASCADE,

  INDEX idx_appointments_time (appointment_time),
  INDEX idx_appointments_doctor_time (doctor_id, appointment_time)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Prescriptions: one header per consultation, with one row per medicine.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prescriptions (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  patient_id     INT          NOT NULL,
  doctor_id      INT          NOT NULL,
  appointment_id INT          NULL,
  issued_at      DATETIME     NOT NULL,
  diagnosis      VARCHAR(255) NULL,
  notes          TEXT         NULL,
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_prescriptions_patient FOREIGN KEY (patient_id)
    REFERENCES patients (id) ON DELETE CASCADE,
  CONSTRAINT fk_prescriptions_doctor FOREIGN KEY (doctor_id)
    REFERENCES doctors (id) ON DELETE CASCADE,
  -- Deleting the appointment keeps the clinical record, but unlinks it.
  CONSTRAINT fk_prescriptions_appointment FOREIGN KEY (appointment_id)
    REFERENCES appointments (id) ON DELETE SET NULL,

  INDEX idx_prescriptions_patient (patient_id),
  INDEX idx_prescriptions_issued (issued_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS prescription_items (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  prescription_id INT          NOT NULL,
  medicine        VARCHAR(120) NOT NULL,
  dosage          VARCHAR(60)  NULL,
  frequency       VARCHAR(60)  NULL,
  duration        VARCHAR(60)  NULL,
  instructions    VARCHAR(255) NULL,

  CONSTRAINT fk_prescription_items_prescription FOREIGN KEY (prescription_id)
    REFERENCES prescriptions (id) ON DELETE CASCADE,

  INDEX idx_prescription_items_parent (prescription_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Billing. Line item amounts are derived (quantity * unit_price) rather than
-- stored, so a total can never disagree with the rows it is made of.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  invoice_number VARCHAR(30)   NOT NULL,
  patient_id     INT           NOT NULL,
  appointment_id INT           NULL,
  issued_at      DATETIME      NOT NULL,
  status         VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
  tax_percent    DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
  notes          VARCHAR(255)  NULL,
  paid_at        DATETIME      NULL,
  created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_invoices_patient FOREIGN KEY (patient_id)
    REFERENCES patients (id) ON DELETE CASCADE,
  CONSTRAINT fk_invoices_appointment FOREIGN KEY (appointment_id)
    REFERENCES appointments (id) ON DELETE SET NULL,

  UNIQUE KEY uq_invoices_number (invoice_number),
  INDEX idx_invoices_status (status),
  INDEX idx_invoices_issued (issued_at)
) ENGINE=InnoDB;

-- Invoice numbers come from this counter, not from MAX() over the invoices
-- table. Deriving them from existing rows would reuse a number after an invoice
-- was deleted, so two different invoices could end up sharing an identity.
CREATE TABLE IF NOT EXISTS invoice_sequence (
  name       VARCHAR(20) PRIMARY KEY,
  next_value INT NOT NULL
) ENGINE=InnoDB;

INSERT INTO invoice_sequence (name, next_value) VALUES ('invoice', 1)
  ON DUPLICATE KEY UPDATE next_value = next_value;

CREATE TABLE IF NOT EXISTS invoice_items (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  invoice_id  INT           NOT NULL,
  description VARCHAR(160)  NOT NULL,
  quantity    INT           NOT NULL DEFAULT 1,
  unit_price  DECIMAL(10,2) NOT NULL DEFAULT 0.00,

  CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id)
    REFERENCES invoices (id) ON DELETE CASCADE,

  INDEX idx_invoice_items_parent (invoice_id)
) ENGINE=InnoDB;

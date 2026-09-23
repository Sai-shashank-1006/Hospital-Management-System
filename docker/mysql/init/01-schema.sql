-- Hospital Management System - MySQL schema and seed data.
--
-- The official MySQL image runs every .sql file in /docker-entrypoint-initdb.d
-- once, on first startup of an empty data volume. To re-run it after changing
-- this file:  docker compose down -v && docker compose up -d

CREATE DATABASE IF NOT EXISTS hospital_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE hospital_db;

CREATE TABLE IF NOT EXISTS patients (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(120) NOT NULL,
  gender        VARCHAR(10)  NOT NULL,
  date_of_birth DATE         NULL,
  phone         VARCHAR(20)  NULL,
  email         VARCHAR(120) NULL,
  address       VARCHAR(255) NULL,
  blood_group   VARCHAR(5)   NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
  created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_doctors_specialization (specialization)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS appointments (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  patient_id       INT          NOT NULL,
  doctor_id        INT          NOT NULL,
  appointment_time DATETIME     NOT NULL,
  reason           VARCHAR(255) NULL,
  status           VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- Deleting a patient or doctor removes their appointments; the UI warns about this.
  CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id)
    REFERENCES patients (id) ON DELETE CASCADE,
  CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id)
    REFERENCES doctors (id) ON DELETE CASCADE,

  INDEX idx_appointments_time (appointment_time),
  INDEX idx_appointments_doctor_time (doctor_id, appointment_time)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Seed data, so a fresh stack shows a populated dashboard straight away.
-- ---------------------------------------------------------------------------

INSERT INTO patients (full_name, gender, date_of_birth, phone, email, address, blood_group) VALUES
  ('Ananya Sharma',  'Female', '1992-03-14', '9876543210', 'ananya.sharma@example.com', '14 Rose Lane, Hyderabad',   'O+'),
  ('Rahul Verma',    'Male',   '1985-11-02', '9812345678', 'rahul.verma@example.com',   '22 Park Street, Pune',      'B+'),
  ('Meera Nair',     'Female', '2001-07-25', '9900112233', 'meera.nair@example.com',    '5 Lake View, Kochi',        'A-'),
  ('Imran Khan',     'Male',   '1978-01-19', '9765432109', 'imran.khan@example.com',    '81 Hill Road, Mumbai',      'AB+'),
  ('Sofia Fernandes','Female', '1996-09-09', '9123456780', 'sofia.f@example.com',       '3 Beach Road, Goa',         'O-');

INSERT INTO doctors (full_name, specialization, phone, email, consultation_fee, available) VALUES
  ('Priya Menon',    'Cardiology',       '9001122334', 'priya.menon@hospital.example',    900.00, TRUE),
  ('Arjun Rao',      'Orthopaedics',     '9002233445', 'arjun.rao@hospital.example',      750.00, TRUE),
  ('Kavita Desai',   'Paediatrics',      '9003344556', 'kavita.desai@hospital.example',   600.00, TRUE),
  ('Sameer Gupta',   'General Medicine', '9004455667', 'sameer.gupta@hospital.example',   450.00, TRUE),
  ('Nisha Patel',    'Dermatology',      '9005566778', 'nisha.patel@hospital.example',    700.00, FALSE);

-- Times are relative to first startup so the dashboard always has future bookings.
INSERT INTO appointments (patient_id, doctor_id, appointment_time, reason, status) VALUES
  (1, 1, DATE_ADD(NOW(), INTERVAL 1 DAY),  'Chest discomfort and palpitations', 'SCHEDULED'),
  (2, 2, DATE_ADD(NOW(), INTERVAL 2 DAY),  'Follow-up on knee injury',          'SCHEDULED'),
  (3, 4, DATE_ADD(NOW(), INTERVAL 3 DAY),  'Persistent fever',                  'SCHEDULED'),
  (4, 1, DATE_SUB(NOW(), INTERVAL 5 DAY),  'Routine cardiac screening',         'COMPLETED'),
  (5, 3, DATE_SUB(NOW(), INTERVAL 2 DAY),  'Childhood vaccination query',       'CANCELLED');

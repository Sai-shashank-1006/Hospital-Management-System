-- Test schema, mirroring docker/mysql/init/01-schema.sql.
-- Kept free of MySQL-only syntax so it loads into H2 running in MySQL mode.

CREATE TABLE patients (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(120) NOT NULL,
  gender        VARCHAR(10)  NOT NULL,
  date_of_birth DATE         NULL,
  phone         VARCHAR(20)  NULL,
  email         VARCHAR(120) NULL,
  address       VARCHAR(255) NULL,
  blood_group   VARCHAR(5)   NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctors (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  full_name        VARCHAR(120)  NOT NULL,
  specialization   VARCHAR(80)   NOT NULL,
  phone            VARCHAR(20)   NULL,
  email            VARCHAR(120)  NULL,
  consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  available        BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE appointments (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  patient_id       INT          NOT NULL,
  doctor_id        INT          NOT NULL,
  appointment_time TIMESTAMP    NOT NULL,
  reason           VARCHAR(255) NULL,
  status           VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id)
    REFERENCES patients (id) ON DELETE CASCADE,
  CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id)
    REFERENCES doctors (id) ON DELETE CASCADE
);

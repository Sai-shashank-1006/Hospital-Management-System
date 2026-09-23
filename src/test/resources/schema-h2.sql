-- Test schema, mirroring docker/mysql/init/01-schema.sql.
-- Kept free of MySQL-only syntax so it loads into H2 running in MySQL mode.

CREATE TABLE patients (
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
  medical_history    VARCHAR(4000) NULL,
  created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctors (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  full_name        VARCHAR(120)  NOT NULL,
  specialization   VARCHAR(80)   NOT NULL,
  phone            VARCHAR(20)   NULL,
  email            VARCHAR(120)  NULL,
  consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  available        BOOLEAN       NOT NULL DEFAULT TRUE,
  available_days   VARCHAR(40)   NULL,
  available_from   TIME          NULL,
  available_to     TIME          NULL,
  room_number      VARCHAR(20)   NULL,
  created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(120) NOT NULL,
  role          VARCHAR(20)  NOT NULL,
  doctor_id     INT          NULL,
  active        BOOLEAN      NOT NULL DEFAULT TRUE,
  last_login_at TIMESTAMP    NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_doctor FOREIGN KEY (doctor_id)
    REFERENCES doctors (id) ON DELETE SET NULL
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

CREATE TABLE prescriptions (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  patient_id     INT           NOT NULL,
  doctor_id      INT           NOT NULL,
  appointment_id INT           NULL,
  issued_at      TIMESTAMP     NOT NULL,
  diagnosis      VARCHAR(255)  NULL,
  notes          VARCHAR(4000) NULL,
  created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_prescriptions_patient FOREIGN KEY (patient_id)
    REFERENCES patients (id) ON DELETE CASCADE,
  CONSTRAINT fk_prescriptions_doctor FOREIGN KEY (doctor_id)
    REFERENCES doctors (id) ON DELETE CASCADE,
  CONSTRAINT fk_prescriptions_appointment FOREIGN KEY (appointment_id)
    REFERENCES appointments (id) ON DELETE SET NULL
);

CREATE TABLE prescription_items (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  prescription_id INT          NOT NULL,
  medicine        VARCHAR(120) NOT NULL,
  dosage          VARCHAR(60)  NULL,
  frequency       VARCHAR(60)  NULL,
  duration        VARCHAR(60)  NULL,
  instructions    VARCHAR(255) NULL,
  CONSTRAINT fk_prescription_items_prescription FOREIGN KEY (prescription_id)
    REFERENCES prescriptions (id) ON DELETE CASCADE
);

CREATE TABLE invoices (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  invoice_number VARCHAR(30)   NOT NULL UNIQUE,
  patient_id     INT           NOT NULL,
  appointment_id INT           NULL,
  issued_at      TIMESTAMP     NOT NULL,
  status         VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
  tax_percent    DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
  notes          VARCHAR(255)  NULL,
  paid_at        TIMESTAMP     NULL,
  created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_invoices_patient FOREIGN KEY (patient_id)
    REFERENCES patients (id) ON DELETE CASCADE,
  CONSTRAINT fk_invoices_appointment FOREIGN KEY (appointment_id)
    REFERENCES appointments (id) ON DELETE SET NULL
);

CREATE TABLE invoice_sequence (
  name       VARCHAR(20) PRIMARY KEY,
  next_value INT NOT NULL
);

INSERT INTO invoice_sequence (name, next_value) VALUES ('invoice', 1);

CREATE TABLE invoice_items (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  invoice_id  INT           NOT NULL,
  description VARCHAR(160)  NOT NULL,
  quantity    INT           NOT NULL DEFAULT 1,
  unit_price  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id)
    REFERENCES invoices (id) ON DELETE CASCADE
);

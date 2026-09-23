-- Demo data, so a fresh stack shows a populated system straight away.
--
-- Staff accounts are NOT seeded here: passwords must be hashed, which SQL cannot
-- do. com.hms.security.UserBootstrap creates the default accounts on first
-- startup when the users table is empty, and logs the credentials once.

USE hospital_db;

INSERT INTO patients
  (full_name, gender, date_of_birth, phone, email, address, blood_group,
   insurance_provider, insurance_number, allergies, medical_history) VALUES
  ('Ananya Sharma',   'Female', '1992-03-14', '9876543210', 'ananya.sharma@example.com', '14 Rose Lane, Hyderabad', 'O+',
   'Star Health',  'SH-4471902', 'Penicillin',   'Hypertension, managed with medication since 2019.'),
  ('Rahul Verma',     'Male',   '1985-11-02', '9812345678', 'rahul.verma@example.com',   '22 Park Street, Pune',    'B+',
   'HDFC Ergo',    'HE-8830145', NULL,           'Arthroscopic knee surgery, 2021.'),
  ('Meera Nair',      'Female', '2001-07-25', '9900112233', 'meera.nair@example.com',    '5 Lake View, Kochi',      'A-',
   'Niva Bupa',    'NB-2219087', 'Sulfa drugs',  'No significant history.'),
  ('Imran Khan',      'Male',   '1978-01-19', '9765432109', 'imran.khan@example.com',    '81 Hill Road, Mumbai',    'AB+',
   'ICICI Lombard','IL-6650321', NULL,           'Type 2 diabetes, diagnosed 2016.'),
  ('Sofia Fernandes', 'Female', '1996-09-09', '9123456780', 'sofia.f@example.com',       '3 Beach Road, Goa',       'O-',
   NULL,           NULL,         'Latex',        'Asthma, seasonal.');

INSERT INTO doctors
  (full_name, specialization, phone, email, consultation_fee, available,
   available_days, available_from, available_to, room_number) VALUES
  ('Priya Menon',  'Cardiology',       '9001122334', 'priya.menon@hospital.example',  900.00, TRUE,
   'MON,TUE,WED,THU,FRI', '09:00:00', '13:00:00', 'C-201'),
  ('Arjun Rao',    'Orthopaedics',     '9002233445', 'arjun.rao@hospital.example',    750.00, TRUE,
   'MON,WED,FRI',         '10:00:00', '16:00:00', 'B-114'),
  ('Kavita Desai', 'Paediatrics',      '9003344556', 'kavita.desai@hospital.example', 600.00, TRUE,
   'TUE,THU,SAT',         '09:30:00', '14:00:00', 'A-005'),
  ('Sameer Gupta', 'General Medicine', '9004455667', 'sameer.gupta@hospital.example', 450.00, TRUE,
   'MON,TUE,WED,THU,FRI,SAT', '08:00:00', '12:00:00', 'A-101'),
  ('Nisha Patel',  'Dermatology',      '9005566778', 'nisha.patel@hospital.example',  700.00, FALSE,
   'TUE,THU',             '11:00:00', '15:00:00', 'D-308');

-- Times are relative to first startup so the dashboard always has future bookings.
INSERT INTO appointments (patient_id, doctor_id, appointment_time, reason, status) VALUES
  (1, 1, DATE_ADD(NOW(), INTERVAL 1 DAY),  'Chest discomfort and palpitations', 'SCHEDULED'),
  (2, 2, DATE_ADD(NOW(), INTERVAL 2 DAY),  'Follow-up on knee injury',          'SCHEDULED'),
  (3, 4, DATE_ADD(NOW(), INTERVAL 3 DAY),  'Persistent fever',                  'SCHEDULED'),
  (4, 1, DATE_SUB(NOW(), INTERVAL 5 DAY),  'Routine cardiac screening',         'COMPLETED'),
  (5, 3, DATE_SUB(NOW(), INTERVAL 2 DAY),  'Childhood vaccination query',       'CANCELLED'),
  (1, 4, DATE_SUB(NOW(), INTERVAL 12 DAY), 'Seasonal flu',                      'COMPLETED'),
  (2, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), 'Blood pressure review',             'COMPLETED');

-- Prescriptions written at the completed consultations.
INSERT INTO prescriptions (patient_id, doctor_id, appointment_id, issued_at, diagnosis, notes) VALUES
  (4, 1, 4, DATE_SUB(NOW(), INTERVAL 5 DAY),  'Stable angina',
   'Continue current statin. Review in three months.'),
  (1, 4, 6, DATE_SUB(NOW(), INTERVAL 12 DAY), 'Influenza A',
   'Rest and fluids. Return if fever persists beyond five days.');

INSERT INTO prescription_items
  (prescription_id, medicine, dosage, frequency, duration, instructions) VALUES
  (1, 'Atorvastatin',  '20 mg', 'Once daily',   '90 days', 'Take at night.'),
  (1, 'Aspirin',       '75 mg', 'Once daily',   '90 days', 'Take after food.'),
  (2, 'Oseltamivir',   '75 mg', 'Twice daily',  '5 days',  'Complete the full course.'),
  (2, 'Paracetamol',   '500 mg','Every 6 hours','5 days',  'Only if temperature is above 38 C.');

-- Billing for the completed consultations.
INSERT INTO invoices
  (invoice_number, patient_id, appointment_id, issued_at, status, tax_percent, notes, paid_at) VALUES
  ('INV-000001', 4, 4, DATE_SUB(NOW(), INTERVAL 5 DAY),  'PAID',   5.00, 'Cardiac screening package',
   DATE_SUB(NOW(), INTERVAL 5 DAY)),
  ('INV-000002', 1, 6, DATE_SUB(NOW(), INTERVAL 12 DAY), 'PAID',   5.00, NULL,
   DATE_SUB(NOW(), INTERVAL 11 DAY)),
  ('INV-000003', 2, 7, DATE_SUB(NOW(), INTERVAL 20 DAY), 'UNPAID', 5.00, 'Awaiting insurance clearance', NULL);

INSERT INTO invoice_items (invoice_id, description, quantity, unit_price) VALUES
  (1, 'Consultation - Cardiology',      1, 900.00),
  (1, 'ECG',                            1, 450.00),
  (1, 'Lipid profile',                  1, 800.00),
  (2, 'Consultation - General Medicine',1, 450.00),
  (2, 'Rapid influenza test',           1, 350.00),
  (3, 'Consultation - Cardiology',      1, 900.00),
  (3, 'Ambulatory BP monitoring',       1, 1200.00);

-- The invoices above were inserted with their numbers written out, which leaves
-- the counter behind them. Move it past the highest seeded number, or the first
-- invoice created through the application collides with INV-000001.
-- Derived from the data rather than hard-coded, so editing the rows above cannot
-- silently put the two back out of step.
UPDATE invoice_sequence
   SET next_value = (
     SELECT COALESCE(MAX(CAST(SUBSTRING(invoice_number, 5) AS UNSIGNED)), 0) + 1
       FROM invoices
      WHERE invoice_number LIKE 'INV-%'
   )
 WHERE name = 'invoice';

-- Run this in SQL*Plus as the same schema user you already use (currently: hostel)
-- It adds authentication users with roles and links them to your existing guests table.

-- 1) Users table
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE app_users (
    user_id        NUMBER PRIMARY KEY,
    email          VARCHAR2(255) NOT NULL,
    full_name      VARCHAR2(120) NOT NULL,
    phone          VARCHAR2(30),
    guest_id       NUMBER,
    role           VARCHAR2(10) NOT NULL,
    password_hash  VARCHAR2(512) NOT NULL,
    password_salt  VARCHAR2(128) NOT NULL,
    iterations     NUMBER NOT NULL,
    created_at     TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT uq_app_users_email UNIQUE (email),
    CONSTRAINT ck_app_users_role CHECK (role IN (''ADMIN'',''USER''))
  )';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- 2) FK link to guests (optional but recommended)
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE app_users ADD CONSTRAINT fk_app_users_guest
    FOREIGN KEY (guest_id) REFERENCES guests(guest_id)';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -2275 THEN RAISE; END IF; -- constraint already exists
END;
/

-- 3) Sequence + trigger for user_id
BEGIN
  EXECUTE IMMEDIATE 'CREATE SEQUENCE app_users_seq START WITH 1 INCREMENT BY 1';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE OR REPLACE TRIGGER app_users_bi
  BEFORE INSERT ON app_users
  FOR EACH ROW
  BEGIN
    IF :NEW.user_id IS NULL THEN
      SELECT app_users_seq.NEXTVAL INTO :NEW.user_id FROM dual;
    END IF;
  END;';
END;
/

-- 4) Create a default ADMIN account
-- NOTE: The password values here are placeholders.
-- After you run the app, use the Admin UI to create users.
-- For the first admin, you can either:
--   A) insert with app-generated hash (recommended), or
--   B) temporarily store a known hash/salt that the app expects.
--
-- I recommend: run the app once, use the 'Create Admin' helper (I will add it in UI)
-- or tell me what admin email/password you want and I'll give you the exact INSERT.

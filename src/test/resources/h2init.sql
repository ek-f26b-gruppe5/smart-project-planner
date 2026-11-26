DROP TABLE IF EXISTS app_user;

CREATE TABLE app_user (
                          id            INT AUTO_INCREMENT PRIMARY KEY,
                          name          VARCHAR(100) NOT NULL,
                          email         VARCHAR(255) NOT NULL UNIQUE,
                          password_hash VARCHAR(100) NOT NULL,
                          role          VARCHAR(20)  NOT NULL DEFAULT 'MANAGER',
                          CONSTRAINT chk_app_user_role CHECK (role IN ('MANAGER', 'EMPLOYEE'))
);

INSERT INTO app_user (id, name, email, password_hash, role) VALUES
                                                                (1, 'Test Manager',     'manager@test.dk',     'hash', 'MANAGER'),
                                                                (2, 'Test Medarbejder', 'medarbejder@test.dk', 'hash', 'EMPLOYEE');

ALTER TABLE app_user ALTER COLUMN id RESTART WITH 100;
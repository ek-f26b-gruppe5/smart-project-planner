DROP TABLE IF EXISTS project;
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

CREATE TABLE project (
                         id          INT AUTO_INCREMENT PRIMARY KEY,
                         manager_id  INT           NOT NULL,
                         name        VARCHAR(100)  NOT NULL,
                         description VARCHAR(1000),
                         start_date  DATE          NOT NULL,
                         deadline    DATE          NOT NULL,
                         CONSTRAINT fk_project_manager FOREIGN KEY (manager_id) REFERENCES app_user (id),
                         CONSTRAINT chk_project_dates  CHECK (deadline >= start_date)
);

INSERT INTO project (id, manager_id, name, description, start_date, deadline) VALUES
    (1, 1, 'Nyt parcelhus', 'Byggeri af et parcelhus', '2027-03-01', '2027-05-28');

ALTER TABLE project ALTER COLUMN id RESTART WITH 100;
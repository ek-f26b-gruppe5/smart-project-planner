CREATE TABLE IF NOT EXISTS app_user (
                                        id            INT AUTO_INCREMENT PRIMARY KEY,
                                        name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE',
    CONSTRAINT chk_app_user_role CHECK (role IN ('MANAGER', 'EMPLOYEE'))
    );
CREATE TABLE IF NOT EXISTS project (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    manager_id  INT           NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(1000),
    start_date  DATE          NOT NULL,
    deadline    DATE          NOT NULL,
    CONSTRAINT fk_project_manager FOREIGN KEY (manager_id) REFERENCES app_user (id),
    CONSTRAINT chk_project_dates  CHECK (deadline >= start_date)
    );
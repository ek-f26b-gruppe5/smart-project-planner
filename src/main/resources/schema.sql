CREATE TABLE IF NOT EXISTS app_user (
                                        id            INT AUTO_INCREMENT PRIMARY KEY,
                                        name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'MANAGER',
    CONSTRAINT chk_app_user_role CHECK (role IN ('MANAGER', 'EMPLOYEE'))
    );
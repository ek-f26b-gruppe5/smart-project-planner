INSERT
IGNORE INTO app_user (id, name, email, password_hash, role) VALUES
    (1, 'Manager',  'manager@spp.dk', '$2a$10$QBjErdFqTnVgPq1hxwBxmOwHGtIAICZs0cqEnh7p3NbPiisJFzI3G', 'MANAGER'),
    (2, 'Employee', 'employee@spp.dk', '$2a$10$QBjErdFqTnVgPq1hxwBxmOwHGtIAICZs0cqEnh7p3NbPiisJFzI3G', 'EMPLOYEE');
INSERT
IGNORE INTO app_user (id, name, email, password_hash, role) VALUES
    (1, 'Manager',  'manager@spp.dk', '$2a$10$LPFX4.jRD2abJ5JC32lbjOi0DDp0lVfaModAeJah9oA0fdE3tT2jC', 'MANAGER'),
    (2, 'Employee', 'employee@spp.dk', '$2a$10$LPFX4.jRD2abJ5JC32lbjOi0DDp0lVfaModAeJah9oA0fdE3tT2jC', 'EMPLOYEE');
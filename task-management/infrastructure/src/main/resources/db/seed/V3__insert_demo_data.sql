-- Demonstration data. The password of the demo user is "demo1234"; the hash below is its BCrypt hash.
-- Due dates are relative to the current date so the demonstration never goes stale: one task is
-- deliberately overdue, which is what makes the "a due date is only validated when it changes" rule
-- visible on an update.
INSERT INTO users (name, email, password_hash)
VALUES ('Demo User', 'demo@example.com', '$2a$10$gGvQpt8nrcm807reybRv.O73QmyX7IMTYrkphuzHIGj2X1DAe73cG');

INSERT INTO tasks (title, description, status, due_date, owner_id, created_at, updated_at)
VALUES ('Write the project plan', 'Phase 1 of the challenge', 'DONE', DATEADD('DAY', -10, CURRENT_DATE),
        (SELECT id FROM users WHERE email = 'demo@example.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Review the open questions', NULL, 'TODO', DATEADD('DAY', -3, CURRENT_DATE),
        (SELECT id FROM users WHERE email = 'demo@example.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Implement the task endpoints', 'Five routes under /api/v1/tasks', 'IN_PROGRESS', DATEADD('DAY', 1, CURRENT_DATE),
        (SELECT id FROM users WHERE email = 'demo@example.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Write the integration tests', NULL, 'IN_PROGRESS', DATEADD('DAY', 7, CURRENT_DATE),
        (SELECT id FROM users WHERE email = 'demo@example.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Deliver the validation report', 'Phase 3 of the challenge', 'TODO', DATEADD('DAY', 14, CURRENT_DATE),
        (SELECT id FROM users WHERE email = 'demo@example.com'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- A help desk. The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE tickets (
    id         bigint PRIMARY KEY,
    title      text NOT NULL,
    body       text NOT NULL,
    status     text NOT NULL CHECK (status IN ('open', 'closed')),
    priority   integer NOT NULL,
    metadata   jsonb NOT NULL DEFAULT '{}',
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE audit_log (
    id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    at        timestamptz NOT NULL DEFAULT now(),
    actor     text NOT NULL DEFAULT current_user,
    op        text NOT NULL,
    ticket_id bigint NOT NULL,
    changes   jsonb NOT NULL
);

INSERT INTO tickets (id, title, body, status, priority, metadata, created_at, updated_at) VALUES
    (1, 'Cannot log in after password reset', 'Every login attempt fails with an error since the reset.', 'open', 3,
        '{"customer": {"plan": "pro"}, "tags": ["auth", "urgent"], "browser": "Firefox"}', '2026-11-01 09:00+00', '2026-11-01 09:00+00'),
    (2, 'Login page shows error 500', 'The login page returns an internal error for all users.', 'open', 5,
        '{"customer": {"plan": "pro"}, "tags": ["auth", "urgent"]}', '2026-11-02 09:00+00', '2026-11-02 09:00+00'),
    (3, 'Invoice total is wrong', 'The invoice shows the wrong total after the discount.', 'open', 2,
        '{"customer": {"plan": "free"}, "tags": ["billing"]}', '2026-11-03 09:00+00', '2026-11-03 09:00+00'),
    (4, 'Logged out every few minutes', 'Users are logged out and see a session error.', 'open', 4,
        '{"customer": {"plan": "enterprise"}, "tags": ["auth"]}', '2026-11-04 09:00+00', '2026-11-04 09:00+00'),
    (5, 'Error exporting reports', 'Exporting to CSV fails with an error.', 'closed', 1,
        '{"customer": {"plan": "pro"}, "tags": ["reports"]}', '2026-11-05 09:00+00', '2026-11-05 09:00+00'),
    (6, 'Login errors on mobile', 'The mobile app shows an error when logging in with SSO.', 'closed', 3,
        '{"customer": {"plan": "pro"}, "tags": ["auth", "mobile"]}', '2026-11-06 09:00+00', '2026-11-06 09:00+00'),
    (7, 'Feature request: dark mode', 'Please add a dark mode.', 'open', 1,
        '{"customer": {"plan": "free"}, "tags": ["ui"]}', '2026-11-07 09:00+00', '2026-11-07 09:00+00'),
    (8, 'Two-factor login error', 'The login code is rejected with an error even when it is correct.', 'open', 2,
        '{"customer": {"plan": "pro"}, "tags": ["auth"]}', '2026-11-08 09:00+00', '2026-11-08 09:00+00'),
    (9, 'Printer error', 'The label printer shows an error.', 'open', 1,
        '{"customer": {"plan": "pro"}, "tags": ["hardware"]}', '2026-11-09 09:00+00', '2026-11-09 09:00+00'),
    (10, 'Slow dashboard', 'The dashboard takes a minute to load.', 'open', 3,
        '{"customer": {"plan": "enterprise"}, "tags": ["performance", "urgent"]}', '2026-11-10 09:00+00', '2026-11-10 09:00+00'),
    (11, 'login', 'Cannot login. Error.', 'open', 1,
        '{"customer": {"plan": "free"}, "tags": ["auth"]}', '2026-11-11 09:00+00', '2026-11-11 09:00+00'),
    (12, 'Billing error after upgrade', 'We were charged twice after the upgrade; error in the billing page.', 'open', 4,
        '{"customer": {"plan": "pro"}, "tags": ["billing", "urgent"]}', '2026-11-12 09:00+00', '2026-11-12 09:00+00');

-- 20,000 routine questions, so that indexes matter.
INSERT INTO tickets (id, title, body, status, priority, metadata, created_at, updated_at)
SELECT i, format('Question about invoice %s', i), 'How do I download the invoice for last month?',
       CASE WHEN i % 3 = 0 THEN 'open' ELSE 'closed' END, 1 + i % 3,
       jsonb_build_object('customer', jsonb_build_object('plan', (ARRAY['free', 'pro', 'enterprise'])[1 + i % 3]),
                          'tags', jsonb_build_array('billing')),
       TIMESTAMPTZ '2026-01-01 00:00+00' + i * INTERVAL '10 minutes',
       TIMESTAMPTZ '2026-01-01 00:00+00' + i * INTERVAL '10 minutes'
FROM generate_series(1001, 21000) AS i;

-- The same expression as the index, on both sides.
SELECT id, email
FROM users
WHERE lower(email) = lower('ANA.SILVA@example.com');

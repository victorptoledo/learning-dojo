-- Fees first: once the loans are closed they are no longer open.
INSERT INTO fees (loan_id, member_id, amount)
SELECT id, member_id, (DATE '2026-11-15' - due_on) * 0.25
FROM loans
WHERE member_id = 3
  AND returned_on IS NULL
  AND due_on < DATE '2026-11-15';

UPDATE loans
SET returned_on = DATE '2026-11-15'
WHERE member_id = 3
  AND returned_on IS NULL;

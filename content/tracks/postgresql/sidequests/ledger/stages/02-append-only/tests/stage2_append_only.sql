-- Stage 2 contract: history.sql makes journal and postings append-only (LD002) and adds
-- reverse_entry(), which cancels an entry once.

-- Alice gets 100 from the bank, then pays bob 25.
CREATE FUNCTION ledger_test.lunch() RETURNS bigint
LANGUAGE sql AS $$
    SELECT post('deposit', '[{"account": 1, "amount": -100}, {"account": 2, "amount": 100}]');
    SELECT post('lunch', '[{"account": 2, "amount": -25.00}, {"account": 3, "amount": 25.00}]');
$$;

SELECT dojo.script_fails('a posting cannot be updated', NULL,
    $$UPDATE postings SET amount = -2500 WHERE amount = -25.00$$, 'LD002',
    'changing an amount must be refused with LD002',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql');
               SELECT ledger_test.lunch()$$);

SELECT dojo.script_fails('a posting cannot be deleted', NULL,
    $$DELETE FROM postings WHERE amount = -25.00$$, 'LD002',
    'deleting a posting must be refused with LD002',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql');
               SELECT ledger_test.lunch()$$);

SELECT dojo.script_fails('an entry cannot be updated', NULL,
    $$UPDATE journal SET memo = 'dinner' WHERE memo = 'lunch'$$, 'LD002',
    'changing a memo must be refused with LD002',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql');
               SELECT ledger_test.lunch()$$);

SELECT dojo.script_fails('an entry cannot be deleted', NULL,
    $$DELETE FROM journal WHERE id = (SELECT max(id) FROM journal)$$, 'LD002',
    'deleting a journal entry must be refused with LD002',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql');
               SELECT ledger_test.lunch()$$);

SELECT dojo.script_fails('postings cannot be truncated', NULL,
    $$TRUNCATE postings$$, 'LD002',
    'TRUNCATE postings must be refused with LD002',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql');
               SELECT ledger_test.lunch();
               SET CONSTRAINTS ALL IMMEDIATE$$);

SELECT dojo.script_fails('the journal cannot be truncated', NULL,
    $$TRUNCATE journal CASCADE$$, 'LD002',
    'TRUNCATE journal must be refused with LD002',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql');
               SELECT ledger_test.lunch();
               SET CONSTRAINTS ALL IMMEDIATE$$);

SELECT dojo.script_ok(
    'new entries are still welcome',
    NULL,
    $$SELECT count(*) = 3 FROM journal$$,
    'posting after the append-only rule should still work',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql')$$,
    after => $$SELECT ledger_test.lunch();
               SELECT post('coffee', '[{"account": 2, "amount": -3}, {"account": 4, "amount": 3}]')$$
);

SELECT dojo.script_eq(
    'a reversal cancels an entry',
    NULL,
    $$SELECT j.memo, j.reverses = (SELECT lunch FROM ids), p.account_id, p.amount, balance(p.account_id)
      FROM journal j JOIN postings p ON p.journal_id = j.id
      WHERE j.id = (SELECT reversal FROM ids) ORDER BY p.account_id$$,
    $$VALUES ('lunch was a mistake', true, 2::bigint, 25.00, 100.00),
             ('lunch was a mistake', true, 3, -25.00, 0)$$,
    ordered => true,
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql')$$,
    after => $$CREATE TEMP TABLE ids AS SELECT ledger_test.lunch() AS lunch, 0::bigint AS reversal;
               UPDATE ids SET reversal = reverse_entry(lunch, 'lunch was a mistake');
               SET CONSTRAINTS ALL IMMEDIATE$$
);

SELECT dojo.script_ok(
    'an ordinary entry reverses nothing',
    NULL,
    $$SELECT bool_and(reverses IS NULL) FROM journal$$,
    'journal.reverses should be NULL for entries written by post',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql')$$,
    after => $$SELECT ledger_test.lunch()$$
);

SELECT dojo.script_fails('an entry is reversed once', NULL,
    $$SELECT reverse_entry((SELECT max(id) FROM journal WHERE reverses IS NULL), 'again')$$, '23505',
    'lunch was already reversed: a second reversal must be a unique violation',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql');
               SELECT reverse_entry(ledger_test.lunch(), 'first')$$);

SELECT dojo.script_fails('a reversal cannot be reversed', NULL,
    $$SELECT reverse_entry((SELECT max(id) FROM journal), 'undo the undo')$$, '22023',
    'the newest entry is a reversal: reversing it must fail with 22023',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql');
               SELECT reverse_entry(ledger_test.lunch(), 'first')$$);

SELECT dojo.script_fails('an unknown entry cannot be reversed', NULL,
    $$SELECT reverse_entry(-1, 'nothing')$$, '22023',
    'entry -1 does not exist',
    setup => $$SELECT ledger_test.require('ledger.sql', 'history.sql')$$);

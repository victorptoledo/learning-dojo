-- The customer portal: a read-only role that sees only the logged-in customer's money.

CREATE ROLE ledger_customer NOLOGIN;
GRANT SELECT ON accounts, journal, postings TO ledger_customer;

-- The logged-in customer, NULL when no request set one ('' after a SET LOCAL ended).
CREATE FUNCTION current_customer() RETURNS text
LANGUAGE sql STABLE AS $$
    SELECT nullif(current_setting('app.customer', true), '')
$$;

ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE postings ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal ENABLE ROW LEVEL SECURITY;

CREATE POLICY own_accounts ON accounts FOR SELECT TO ledger_customer
    USING (owner = current_customer());

-- The subqueries read accounts and postings with their own policies applied.
CREATE POLICY own_postings ON postings FOR SELECT TO ledger_customer
    USING (account_id IN (SELECT id FROM accounts));

CREATE POLICY own_entries ON journal FOR SELECT TO ledger_customer
    USING (EXISTS (SELECT FROM postings p WHERE p.journal_id = journal.id));

-- SECURITY INVOKER (the default): it reads with the caller's rights, policies included.
CREATE FUNCTION statement(p_account bigint, p_from date, p_to date)
RETURNS TABLE (posted_at timestamptz, memo text, amount numeric, balance numeric)
LANGUAGE sql STABLE AS $$
    SELECT posted_at, memo, amount, running
    FROM (SELECT j.posted_at, j.memo, p.amount,
                 sum(p.amount) OVER (ORDER BY j.posted_at, p.id) AS running, p.id
          FROM postings p JOIN journal j ON j.id = p.journal_id
          WHERE p.account_id = p_account AND j.posted_at < p_to + 1) AS history
    WHERE posted_at >= p_from
    ORDER BY posted_at, id
$$;

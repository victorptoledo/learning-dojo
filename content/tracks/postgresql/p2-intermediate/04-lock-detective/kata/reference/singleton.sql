-- Never waits, and the lock goes away with the transaction, whatever happens.
SELECT pg_try_advisory_xact_lock(4242) AS got_lock;

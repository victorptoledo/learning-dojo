# Job Queue: Claiming Work Safely with SKIP LOCKED

Many systems keep background jobs (emails, reports, webhooks) in a table and run several
workers that take jobs from it. The hard part is concurrency: two workers must never take the
same job, and a busy worker must not block the others. Press **Enter** to create the kata
folder and open it.

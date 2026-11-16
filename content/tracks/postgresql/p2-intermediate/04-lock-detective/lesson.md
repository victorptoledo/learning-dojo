# Lock Detective: Blocking Chains, Safe Migrations and Advisory Locks

An idle transaction, a migration and a queue of waiting queries: the most common PostgreSQL
outage. In this kata real sessions block each other while you find the whole chain, end the
session at its root, write a migration that gives up instead of queueing, and use an advisory
lock so only one instance runs a job. Press **Enter** to create the kata folder and open it.

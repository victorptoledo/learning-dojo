# Vacuum Clinic: HOT Updates, Autovacuum Triggers and Wraparound

The previous two lessons explained row versions, VACUUM and freezing. This kata turns them into
on-call work: design a table whose constant updates stay HOT, read a production export to find
the tables autovacuum is neglecting, tune it per table, and check how far every database is from
transaction id wraparound. Press **Enter** to create the kata folder and open it.

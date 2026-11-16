---
from: csharp
status: draft
concept: "Capturing database changes"
from_way: "SQL Server triggers or application code that writes audit rows."
native_way: "SQL Server CDC or Debezium reading the transaction log, and an outbox for domain events."
sensei_tip: "SQL Server has CDC built in (sys.sp_cdc_enable_table) with change tables and LSNs; Debezium's SQL Server connector reads those change tables."
---

| C# | Change data capture |
| --- | --- |
| AFTER UPDATE trigger | a hand-made, costly capture |
| sys.sp_cdc_enable_table | SQL Server log-based CDC |
| __$start_lsn | the log position |
| SaveChanges with an outbox entity | the outbox pattern |
| Change Tracking | which rows changed, without the values |

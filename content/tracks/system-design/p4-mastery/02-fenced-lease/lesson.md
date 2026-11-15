# Fenced Lease

Build a lease manager that hands out expiring leases with strictly increasing fencing tokens,
and the storage that uses those tokens to reject a holder who woke up from a pause after
losing its lease. The tests replay the classic GC-pause accident and check that this time
nothing is corrupted. Press **Enter** to create the kata folder and open it.

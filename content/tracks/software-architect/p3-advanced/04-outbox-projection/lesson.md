# Outbox and Projection

"Save the order, then publish the event" is two writes to two systems, and there is no
transaction that covers both: a crash in between loses the event, and a publish before a rollback
announces an order that never existed. In this kata you write the event into an **outbox** table
in the same transaction as the order, relay it to the broker **at least once**, and build a read
model whose consumer shrugs off the duplicates that at-least-once delivery brings. Press
**Enter** to create the kata folder and open it.

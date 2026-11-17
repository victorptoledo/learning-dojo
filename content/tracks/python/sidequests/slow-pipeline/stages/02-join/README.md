**`enrich(orders, customers)`** adds each order's customer country (`"unknown"` for a
customer that is not in the list).

Budget: **one second** for 20 000 orders and 10 000 customers. This is a join; think of how a
database runs one when it has an index, and build that index once.

Keep everything else the same: a new list of new dicts, in the orders' order, and the input
dicts left untouched. When two customers share an id, the **first** one counts.

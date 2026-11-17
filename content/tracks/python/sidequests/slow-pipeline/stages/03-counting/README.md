**`top_products(orders, n)`** returns the `n` products sold in the largest quantity, with
their quantities, ties broken by product name.

Budget: **one second** for 50 000 orders of 5 000 different products. How many times does the
current version walk over all the orders? One pass is enough; `collections.Counter` and
`heapq` are your friends.

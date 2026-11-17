**`unique_customers(orders)`** returns the customer ids in the order they first appear.

Time it on 50 000 orders from 20 000 different customers. The contract test gives it **one
second** for that; the current version needs far more. Look at what `x not in some_list`
costs, and how often it runs.

The order of the result must stay the order of first appearance.

# Break the Cycle

Two packages that depend on each other are one package with two names: you cannot build, test,
reuse or split either one alone. Here `orders` calls `billing` to invoice an order, and
`billing` reaches back into `orders` to read the order and to mark it paid. You will keep one
direction and invert the other, so the package graph becomes a line again, with every invoice
and payment rule unchanged. Press **Enter** to create the kata folder and open it.

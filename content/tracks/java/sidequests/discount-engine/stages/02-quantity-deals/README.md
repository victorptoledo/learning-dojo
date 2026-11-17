Two quantity deals, still one discount per line (the largest wins, as before):

| Rule | Effect on a line of that SKU |
|---|---|
| `buy <x> get <y> sku <sku>` | for every full group of `x + y` units, `y` units are free |
| `bulk <min> price <cents> sku <sku>` | from `min` units on, every unit costs `cents` (when that is cheaper) |

Buying 7 pens with `buy 2 get 1 sku PEN` makes 2 of them free (two groups of 3).

Bad rules are now rejected by `parse`, with an `IllegalArgumentException` whose message
starts with `line <n>: ` (lines counted from 1, blank and comment lines included):

- an unknown rule (`teleport 3 sku A`);
- the wrong words or number of words (`buy 2 get sku A`);
- a number that is not a whole number, or not positive;
- a `percent` above 100.

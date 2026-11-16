# Extract a Bounded Context

"Product" means one thing to the people who sell it (a name and a price) and another to the
people who store it (a weight, a bin, a count). One class for both means both teams edit it,
both teams' rules live in it, and every change needs a meeting. In this kata you split it:
each context gets its own model and its own store, and sales asks the warehouse for stock
through an interface the warehouse publishes. Press **Enter** to create the kata folder and
open it.

Not everyone had everything. Every row of the `Items` list now says who had it.

- When there is somebody at the table, each item row has a group named `Who had <item>?`
  (a `<fieldset>` with that `<legend>`) with one check box per person, labelled with their
  name, in table order.
- A new item is ticked for everyone at the table at that moment. Someone added later is not
  ticked on the items that were already there, but is on the next ones. Removing a person
  removes them from every item, and someone removed and added again is a new person.
- An item nobody had says `Nobody had <item>` in its row.

**Who owes what.** Under the subtotal, a list named `Shares` has one row per person, in
table order: `<name> owes <amount>` (`Ana owes 6.25`). It is not shown when the table is
empty.

- An item is split evenly among the people who had it. When the cents do not divide, the
  ones left over go one each to the first of those people **in table order** (10.00 for
  three: 3.34, 3.33, 3.33).
- The price of items nobody had is shown as `Unassigned: <amount>`, only when it is more
  than zero.
- The shares and the unassigned amount always add up to the subtotal, exactly.

Time for the tip.

- A group named `Tip` (a `<fieldset>` with that `<legend>`) has four radio buttons, in this
  order: `No tip` (chosen at first), `10%`, `15%` and `Custom`.
- `Custom` shows a text field labelled `Custom tip (%)`, and only while `Custom` is chosen.
  It takes a whole number from 0 to 100; empty means 0. Anything else (`101`, `2.5`, `abc`)
  shows `Enter a tip from 0 to 100`, tied to the field as in stage 1, while you type, and
  the tip counts as 0 until it is fixed.
- Under the subtotal: `Tip: <amount>` and `Total: <subtotal plus tip>`. The tip is the
  percentage of the subtotal, rounded to the nearest cent, half a cent up (15% of 0.10 is
  0.02).

**The tip in the shares.** Each row of `Shares` now includes the person's part of the tip,
in proportion to what they had: someone who had 30.00 of a 40.00 bill pays three quarters of
it. Split the tip in cents: each person gets the whole cents of their exact part, and the
cents left over go one each to the people with the **largest** fractions left, ties in table
order. When nobody has a share (all items unassigned), the tip is added to `Unassigned`.

The rule from stage 2 still holds, now with the tip: the shares plus `Unassigned` are always
exactly the `Total`.

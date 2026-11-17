# Sidequest: Bill of materials

A small factory builds two bicycles. A bike is an **assembly** of other parts (a frame, two
wheels, a drivetrain...), those are assemblies too, and at the bottom are the **components**
the factory buys. The parts tree is stored the classic way, one row per direct use:

```text
parts     (id, sku, name, kind: 'assembly' | 'component', unit_cost: of components only)
bom       (parent_id → parts, child_id → parts, quantity)   -- parent uses quantity × child
suppliers (id, name)
prices    (part_id → parts, supplier_id → suppliers, price, valid_from)
stock     (part_id → parts, on_hand)
```

Everything is in `fixture/01_factory.sql`; the dojo loads it before every check. Questions
about a tree of unknown depth need a **recursive CTE**; questions about "the previous row",
"the best row of each group" or "the total so far" need **window functions**. Over four
stages you write one query per file:

1. explode a bike into its components, and find every part that uses a component;
2. roll the costs up through the tree, and print the tree indented;
3. detect cycles and inconsistent parts before they break the other queries;
4. find the current prices, track their changes and plan a purchase with a running total.

How it is checked:

- Each `.sql` file at the top of the folder is **one query**; the tests compare its rows
  (values and order) with the expected ones. They also change the fixture first (a part
  added, a quantity changed, a cycle introduced) to check that your query follows the data,
  so nothing can be typed in by hand.
- Do not edit `fixture/`; write plain queries (no `psql` commands).
- Try them with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play` from
  this folder, then `\i explode.sql`.

When you are done, `[A]` copies a review prompt: ask an AI about your queries.

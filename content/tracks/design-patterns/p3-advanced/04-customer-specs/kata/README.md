# Customer Specs

Every question the business asks about customers became one more finder in
`CustomerRepository`, each repeating `active`, `country` and `orders` checks in a new
combination. `CustomerQueries` writes the same conditions a second time as SQL, gluing values
into the text, so a country called `x' OR '1'='1` reads every row.

Refactor it to the **Specification** pattern, the Java 25 way: a question is a **value**,
built from a small sealed family of records and combined with `and`, `or` and `not`. The same
spec then has two **interpreters**: one answers it in memory, the other turns it into a
parameterised SQL `WHERE` clause. A new question needs no new code; a new interpreter (say,
an Elasticsearch query) needs no change to the specs.

The finders stay for their callers (the visible tests pin them and pass before you start), but
each becomes one line. `CustomerQueries` changes its API: its old methods go away.

## The target design

```java
public sealed interface CustomerSpec permits IsActive, InCountry, MinOrders, And, Or, Not {
    static CustomerSpec active()                    // new IsActive()
    static CustomerSpec inCountry(String country)   // new InCountry(country)
    static CustomerSpec minOrders(int count)        // new MinOrders(count): orders >= count
    default CustomerSpec and(CustomerSpec other)    // new And(this, other)
    default CustomerSpec or(CustomerSpec other)     // new Or(this, other)
    default CustomerSpec not()                      // new Not(this)
    default boolean isSatisfiedBy(Customer customer) // the in-memory interpreter
}
```

| Type | What it does |
| --- | --- |
| records `IsActive()`, `InCountry(String country)`, `MinOrders(int count)`, `And(left, right)`, `Or(left, right)`, `Not(spec)` | the six specs, each a top-level record implementing `CustomerSpec` |
| `record SqlWhere(String clause, List<Object> params)` | a clause with `?` placeholders and its values in order |
| `CustomerQueries.where(CustomerSpec spec)` | the SQL interpreter, rules below |
| `CustomerRepository.findAll(CustomerSpec)` / `count(CustomerSpec)` | the matching customers in insertion order / how many |

SQL rendering:

| Spec | Clause | Params |
| --- | --- | --- |
| `IsActive` | `active = TRUE` | none |
| `InCountry(c)` | `country = ?` | `c` |
| `MinOrders(n)` | `orders >= ?` | `n` (an `Integer`) |
| `And(l, r)` | `(` l ` AND ` r `)` | l's, then r's |
| `Or(l, r)` | `(` l ` OR ` r `)` | l's, then r's |
| `Not(s)` | `NOT ` s, with s wrapped in parentheses unless it already starts with `(` | s's |

So `active().and(inCountry("PT")).and(minOrders(10))` renders
`((active = TRUE AND country = ?) AND orders >= ?)` with params `["PT", 10]`.

## What the grading tests check

- The old finders, and new questions answered by `findAll` and `count` alone.
- The combinators build plain, comparable records.
- The SQL clauses and parameter order above; a hostile value stays in `params`, never in the
  text.
- `CustomerSpec` is sealed and permits exactly the six records.
- `CustomerRepository` contains no conditions (`.active()`, `.country()`, `.orders()`, loops,
  `if`); both interpreters use a `switch` without `instanceof` or a `default` branch, and
  `CustomerQueries` has no `'` anywhere.

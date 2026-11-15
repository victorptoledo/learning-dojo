# Specification and Strategy as Functions

Repositories grow finders: `findActive`, `findActiveInCountry`,
`findActiveInCountryWithAtLeastOrders`, `findDormant` ... each repeating the same conditions
in a new combination, and then the same conditions again in SQL, in validation and in reports.
The **specification** pattern (Eric Evans and Martin Fowler) turns a business rule into a
**value** you can name, combine, test and reuse.

## Step 1: rules as functions

In Java, the smallest specification is a `Predicate<T>`:

```java
Predicate<Customer> active = Customer::active;
Predicate<Customer> inPortugal = c -> c.country().equals("PT");
List<Customer> found = customers.stream().filter(active.and(inPortugal)).toList();
```

`and`, `or` and `negate` are built in. This is **strategy as a function**: the rule is passed
to a generic algorithm (`filter`). For in-memory selection with a few rules, stop here.

## Step 2: named specifications

A specification adds a **name** and a place in the domain:

```java
interface Specification<T> {
    boolean isSatisfiedBy(T candidate);
    default Specification<T> and(Specification<T> other) { return c -> isSatisfiedBy(c) && other.isSatisfiedBy(c); }
    // or, not
}

final class CustomerSpecs {
    static Specification<Customer> loyal(int minOrders) { return c -> c.orders() >= minOrders; }
}
```

Named rules read like the business (`active().and(loyal(10))`), can be unit tested one by one,
and are reused by selection (find the matching customers), **validation** (does this one
satisfy the rule?) and **construction** (build a candidate that satisfies it).

## Step 3: specifications as data

A lambda is opaque: it can be evaluated, but not turned into SQL, explained to a user or
serialised. When the same rule must be **evaluated in memory and translated into a query**, make
the specification **data** and write interpreters:

```java
sealed interface CustomerSpec permits IsActive, InCountry, MinOrders, And, Or, Not { ... }
record InCountry(String country) implements CustomerSpec {}
record And(CustomerSpec left, CustomerSpec right) implements CustomerSpec {}

boolean isSatisfiedBy(Customer c)  // switch over the records: in memory
SqlWhere where(CustomerSpec spec)  // switch over the records: "(active = TRUE AND country = ?)", ["PT"]
```

This is the composite (specs contain specs) plus the interpreter pattern, made safe by sealed
types: a new interpreter (an explanation, an Elasticsearch query) is one new function; a new
kind of rule forces every interpreter to handle it, and the compiler lists them. Values always
go into **parameters**, never into the query text, so specifications built from user input
cannot inject SQL. You build exactly this in the next kata.

## Spring Data's Specification

Spring Data JPA ships a specification type that is a function producing a JPA criteria
`Predicate`:

```java
static Specification<Customer> inCountry(String country) {
    return (root, query, cb) -> cb.equal(root.get("country"), country);
}

customerRepository.findAll(where(active()).and(inCountry("PT")));   // JpaSpecificationExecutor
```

It solves query composition for JPA with less code, at the cost of tying the rule to JPA's
criteria API (it cannot be evaluated in memory without a database). Querydsl predicates and
jOOQ conditions play the same role. Choose by who needs the rule: only queries → Spring Data's
type; domain logic and queries → your own specification and an adapter that translates it.

## Where specifications pay off

- Many combinations of a few conditions (search screens, campaign targeting, eligibility).
- The same rule in several places (validation, selection, reporting).
- Rules that business people name and change (`premiumCustomer`, `riskyTransaction`).

Where they do not: one or two fixed queries. A clearly named repository method is simpler.

## Further reading

- Eric Evans and Martin Fowler, "Specifications" (the original paper): <https://martinfowler.com/apsupp/spec.pdf>
- Spring Data JPA reference, "Specifications": <https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html>
- Java SE 25 API, package `java.util.function` (`Predicate.and`, `or`, `negate`): <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/function/package-summary.html>
- Wikipedia, "Expression problem" (why sealed data and interpreters trade off): <https://en.wikipedia.org/wiki/Expression_problem>
- Refactoring.Guru, "Composite": <https://refactoring.guru/design-patterns/composite>

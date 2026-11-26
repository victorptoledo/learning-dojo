# Flight Network

A fare search site keeps airports joined by **one-way routes**, each with a price. You write
`FlightNetwork`, which answers the questions the site asks all day: are two airports in the same
region, what is the cheapest fare between them, what is the cheapest fare if the traveller
accepts at most k flights, which airports match what the user is typing, and what would the
cheapest network that joins every airport cost.

```java
var net = new FlightNetwork();
net.addRoute("LIS", "MAD", 40);
net.addRoute("MAD", "PAR", 60);
net.addRoute("LIS", "PAR", 150);
net.addRoute("PAR", "BER", 50);
net.addRoute("LIS", "LON", 70);
net.addRoute("LON", "BER", 120);
net.addAirport("OSL");
net.connected("BER", "LIS");          // true  (routes count in either direction)
net.cheapest("LIS", "BER");           // 150   (LIS → MAD → PAR → BER, three flights)
net.cheapest("BER", "LIS");           // -1    (routes are one-way)
net.cheapestWithin("LIS", "BER", 2);  // 190   (LIS → LON → BER)
net.suggest("L", 5);                  // [LIS, LON]  (LIS has 3 routes, LON 2)
net.networkCost();                    // -1    (nothing reaches OSL)
```

## Airports and routes

- An airport code is 1 to 8 letters `A`–`Z`. Anything else (`null`, empty, lower case, digits,
  accents, spaces, 9 letters) throws `IllegalArgumentException`.
- `addAirport(code)` adds an airport with no routes; adding a known code does nothing.
- `addRoute(from, to, price)` adds a one-way route and any airport that is new. A route from an
  airport to itself or a negative price throws `IllegalArgumentException`, and a refused route
  adds nothing, not even its airports. Two routes may join the same airports: both count.
- Every question below throws `IllegalArgumentException` for an unknown airport.

## Questions

| Method | Returns |
| --- | --- |
| `connected(a, b)` | whether a chain of routes joins them **ignoring direction** (true for `a` with itself) |
| `cheapest(from, to)` | the lowest total price of a sequence of routes from `from` to `to`; 0 when they are the same; -1 if there is none |
| `cheapestWithin(from, to, maxFlights)` | the same, using at most `maxFlights` routes; a negative limit throws `IllegalArgumentException` |
| `suggest(prefix, limit)` | up to `limit` codes that start with `prefix` (which may be empty), the airport with the **most routes** (in or out) first, ties by code; a prefix that is not letters `A`–`Z` or a negative limit throws `IllegalArgumentException` |
| `networkCost()` | the lowest total price of a set of routes, each used **in both directions**, that joins every airport; 0 with fewer than two airports; -1 if some airport cannot be joined |

## How it is graded

This is the integration kata of the Advanced phase. The hidden tests compare every answer with
slow references on hundreds of random small networks, then scale up:

- 200,000 `connected` questions while 200,000 routes arrive, starting with a chain of 100,000
  (searching the graph on each question, or a union-find without its two optimisations, is too
  slow);
- 50,000 airports and 350,000 routes for `cheapest` (Bellman-Ford over every airport is too slow),
  `cheapestWithin` with limits of 3 and 8, and `networkCost`;
- a chain of 60,000 airports asked with `maxFlights = Integer.MAX_VALUE`;
- 60,000 suggestions among 140,000 airports (scanning every code is too slow).

> Tip: four of the five questions are classics from this phase. The new one is
> `cheapestWithin`: the cheapest way to an airport may use too many flights, so "the best price
> so far" per airport is not enough state. What does the state need, and in which order can you
> fill it?

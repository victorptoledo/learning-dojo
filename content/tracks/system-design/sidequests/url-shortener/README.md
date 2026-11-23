# Sidequest: URL shortener

"Design a URL shortener" is the first question of many system design interviews. Here you
build the parts the whiteboard version waves away: where the codes come from when many
servers create links at once, how a cache keeps the slow database out of the redirect path
(also for codes that do not exist), and how one noisy client is kept from using up the
service for everybody else.

Build `dojo.shortener.Shortener` and, in the last stage, `dojo.shortener.Gateway`. The
starter gives you the contract types:

- `LinkStore`, the database: `putIfAbsent`, `get` and `delete`. Think of every call as a
  network round trip of a few milliseconds. The tests pass a store in memory that counts the
  calls.
- `IdAllocator`, the ticket server: `reserve(count)` hands out a fresh range of `count` ids
  that no other server will ever get. Every call is a round trip too.
- `ShortenerConfig`, the numbers of the design (block size, code length, cache size and
  times).

Time comes from the `java.time.Clock` given to the constructor: never call `Instant.now()`
directly. The tests move the clock by hand, so a cache entry that lives ten minutes expires
in no time at all.

There is no real network or database: the parts talk through the interfaces above, which is
what makes every behaviour testable and every round trip countable.

Use the JDK only.

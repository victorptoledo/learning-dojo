A container that loops forever or says "error" is worse than none.

- A cycle (`A` needs `B`, `B` needs `A`) is detected before it overflows the stack: a
  `ContainerException` whose message contains the path with simple names, from the class asked
  for back to the repeated one: `cycle: A -> B -> A`. A class that needs itself is
  `cycle: A -> A`.
- When something fails deep in a graph (a missing class, a missing binding, a constructor
  that throws), the message also shows the path that led there, as `A -> B -> C`, where `C`
  is the class that failed. The cause of a throwing constructor is still kept.
- A failed `get` leaves the container usable: a later `get` of something else works, and
  half-built singletons are not kept.

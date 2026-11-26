# Lazy Sequences: Generators with Precise Types

Write a small library of lazy sequence helpers on generators: `range`, `map`, `filter` with a
type-guard overload, `take`, `chunk`, a variadic `zip` with one type per source, `pairwise`
and a re-iterable `Seq` pipeline. The tests count how many items each helper pulls and check
that sources are closed when a helper stops early. Press **Enter** to create the kata folder
and open it.

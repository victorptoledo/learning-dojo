# A Fitness Function Suite

Everything works on the graph that `ClassGraph.scan(root)` returns (class → classes it uses) and
on `ClassGraph.abstractTypes(root)` (the interfaces and abstract classes). Only **scanned**
classes count: the JDK and libraries never do.

## Part 1: package metrics

`Metrics.of(classGraph, abstractTypes)` returns, for every package that holds a scanned class, a
`PackageMetrics(pkg, afferent, efferent, instability, abstractness, distance)`:

| Metric | Definition |
| --- | --- |
| afferent coupling `Ca` | the number of scanned classes **outside** the package that use at least one class inside it |
| efferent coupling `Ce` | the number of scanned classes outside the package that classes inside it use |
| instability `I` | `Ce / (Ca + Ce)`, and `0` when both are `0` |
| abstractness `A` | abstract types in the package / classes in the package |
| distance `D` | `abs(A + I - 1)`: how far the package is from the main sequence |

The result is keyed by package name and sorted.

## Part 2: cycles

`Cycles.find(packageGraph)` takes the graph of `ClassGraph.packageDependencies` and returns
every package cycle as a list of package names: each group of two or more packages that all
reach each other (a strongly connected component). Names inside a group are sorted, and the
groups are sorted by their first name. No cycle: an empty list.

## Part 3: the ratchet

A legacy codebase breaks rules today. Failing the build on all of them stops the team; ignoring
them lets them grow. `Ratchet.check(baseline, current)` returns
`RatchetResult(newViolations, fixed)`, both sorted lists: `newViolations` are in `current` but
not in the baseline (the build fails, `passes()` is false); `fixed` are in the baseline but no
longer in `current` (the build passes, and you should remove them from the baseline so they
cannot come back).

## Part 4: the suite

`FitnessSuite.violations(classGraph, abstractTypes)` returns the sorted set of:

- `"cycle: <p1>, <p2>, ..."` for each cycle from Part 2;
- `"main sequence: <pkg> D=<distance>"` for each package with `D > 0.7`, the distance with two
  decimals (`String.format(Locale.ROOT, "%.2f", d)`).

`SuiteTest` runs the suite on the small app in `dojo.app` and ratchets it against
`src/test/resources/fitness-baseline.txt`, one violation per line. The baseline is given and
matches the app as it is.

## What the grading tests check

- The metrics, the cycles, the ratchet and the suite on small graphs, including JDK classes that
  must be ignored, packages with no dependencies, self-contained cycles and two separate cycles.
- The suite on the real app, read from its compiled classes.

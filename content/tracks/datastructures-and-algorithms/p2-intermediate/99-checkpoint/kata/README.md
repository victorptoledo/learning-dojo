# Build Planner

A build tool (think Maven, Gradle or Bazel) has tasks, each with a duration and a list of tasks
it **depends on**. You write `BuildPlanner`, which answers the questions such a tool asks: in
which order can the tasks run, is there a dependency cycle, when can each task finish if
everything independent runs in parallel, which chain of tasks decides the total time, and what
must be rebuilt when one task changes.

```java
var plan = new BuildPlanner();
plan.addTask("compile", 30, List.of());
plan.addTask("test", 60, List.of("compile"));
plan.addTask("docs", 20, List.of("compile"));
plan.addTask("package", 10, List.of("test", "docs"));
plan.order();                 // [compile, docs, test, package]
plan.earliestFinish("test");  // 90   (30 + 60)
plan.totalTime();             // 100  (compile → test → package)
plan.criticalPath();          // [compile, test, package]
plan.affectedBy("compile");   // [docs, package, test]
```

## Adding tasks

`addTask(name, duration, dependsOn)`:

- A `null` or blank name, a duplicate name, a negative duration or a task that depends on itself
  throws `IllegalArgumentException`. Repeated names in `dependsOn` count once.
- A dependency may name a task that is added **later**. A dependency that is still unknown when
  you ask a question below makes that question throw `IllegalStateException`.

## Questions

| Method | Returns |
| --- | --- |
| `order()` | every task, each after all of its dependencies. When several tasks are ready, the **alphabetically smallest** goes first, so the order is unique. A cycle throws `IllegalStateException`. |
| `findCycle()` | `Optional.empty()` when there is no cycle; otherwise one cycle as a list that starts and ends with the same task, where each task depends on the next: `[a, b, c, a]` means a depends on b, b on c and c on a |
| `earliestFinish(task)` | its duration plus the latest finish among its dependencies (0 for none) |
| `totalTime()` | the latest finish of any task; 0 with no tasks |
| `criticalPath()` | the chain that sets `totalTime`, first task first. Start from the task with the latest finish and repeatedly step to the dependency with the latest finish; break ties by the alphabetically smallest name. Empty with no tasks. |
| `affectedBy(task)` | every task that depends on it directly or indirectly, sorted, without the task itself |

An unknown task name passed to `earliestFinish` or `affectedBy` throws
`IllegalArgumentException`. `earliestFinish`, `totalTime` and `criticalPath` throw
`IllegalStateException` when there is a cycle.

## How it is graded

This is the integration kata of the Intermediate phase. The hidden tests compare every answer
with slow references on random graphs, then use builds of 200,000 tasks: long chains (a
recursive DFS overflows the stack), ladders where each task depends on the previous two (an
unmemoised recursion is exponential), and wide graphs (scanning all tasks for the next ready one
is quadratic).

> Tip: compute everything at once, lazily, and cache it until the next `addTask`: a reverse
> adjacency list (who depends on me), in-degrees, Kahn's algorithm with a `PriorityQueue` of
> ready names, and the finish times in that order. You may use `java.util.PriorityQueue` here.

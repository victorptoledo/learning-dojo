# The JVM Under Load: Memory, GC and Profiling

Most Java performance problems are not solved by clever code. They are solved by knowing
what the JVM is doing: where the memory goes, what the garbage collector costs, and which
method is actually slow. This lesson gives you the model and the tools.

## Where things live

| Area | Holds | Size set by |
| --- | --- | --- |
| **Heap** | every object and array | `-Xms` (initial), `-Xmx` (maximum), or a percentage of RAM |
| **Thread stacks** | local variables and call frames, per platform thread | `-Xss` (virtual threads keep small stacks on the heap) |
| **Metaspace** | class metadata | grows as classes load; `-XX:MaxMetaspaceSize` |
| **Code cache** | machine code produced by the JIT | `-XX:ReservedCodeCacheSize` |
| **Direct memory** | off-heap buffers (NIO, Netty) | `-XX:MaxDirectMemorySize` |

A local `int` lives on the stack; `new Order(...)` lives on the heap, and the variable on the
stack holds a reference to it. An object can be collected once nothing reachable points to it.
Since Java 25, **compact object headers** (`-XX:+UseCompactObjectHeaders`) shrink every
object's header from 12 to 8 bytes, which saves memory in heaps full of small objects.

## Garbage collection

The garbage collector finds objects that are no longer reachable and reclaims their memory.
It relies on the **generational hypothesis**: most objects die young. Young objects are
collected often and cheaply; the few survivors move to the old generation, collected rarely.

What you trade:

- **Throughput**: the share of time spent running your code instead of collecting.
- **Latency**: how long the application pauses during a collection.
- **Footprint**: how much memory the heap needs to keep both acceptable.

| Collector | Flag | Best for |
| --- | --- | --- |
| **G1** (default) | `-XX:+UseG1GC` | general services; pauses usually tens of milliseconds |
| **ZGC** (generational) | `-XX:+UseZGC` | large heaps and strict latency; pauses under a millisecond |
| **Parallel** | `-XX:+UseParallelGC` | batch jobs that only care about throughput |
| **Serial** | `-XX:+UseSerialGC` | tiny heaps and containers with one CPU |

Start with the default G1. Switch to ZGC when pause times matter more than a little extra CPU
and memory. Changing collectors without measuring is guesswork.

### In containers

The JVM reads the container's CPU and memory limits. By default the maximum heap is 25% of
the available memory, which is usually too little for a container that only runs the JVM:

```text
-XX:MaxRAMPercentage=75
```

Leave the rest for metaspace, thread stacks, direct buffers and the operating system. An
`OutOfMemoryError` in the logs means the heap is too small **or** something keeps references
it should drop (a leak); the container being killed without that error means the whole
process went over the limit.

## The JIT and warm-up

The JVM starts by **interpreting** bytecode, finds the **hot** methods, and compiles them to
machine code with the C1 and then C2 compilers, using what it observed (which branches run,
which types arrive) to optimise. That is why a Java service is slower in its first seconds,
and why micro-benchmarks written with a simple loop and `System.nanoTime` lie: they measure
the interpreter, or code the JIT removed as dead.

Java 24 and 25 added an **AOT cache** that records class loading and method profiles from a
training run and reuses them at startup (`-XX:AOTCacheOutput=app.aot` while training, then
`-XX:AOTCache=app.aot`), which shortens warm-up without leaving the JVM.

## Measuring: JMH

For micro-benchmarks use **JMH**, the harness the JDK team uses. It handles warm-up, forks
fresh JVMs, and stops the JIT from optimising your benchmark away:

```java
@State(Scope.Benchmark)
public class JoinBenchmark {
    List<String> words = IntStream.range(0, 1_000).mapToObj(i -> "w" + i).toList();

    @Benchmark
    public String stream() {
        return words.stream().collect(Collectors.joining(","));
    }

    @Benchmark
    public String builder() {
        var sb = new StringBuilder();
        for (String w : words) sb.append(w).append(',');
        return sb.toString();
    }
}
```

## Profiling: Java Flight Recorder

**JFR** is built into the JVM and cheap enough to leave on in production (about 1% overhead).
It records CPU samples, allocations, locks, GC pauses, I/O and virtual thread events.

```text
java -XX:StartFlightRecording=duration=60s,filename=app.jfr -jar app.jar
jcmd <pid> JFR.start duration=60s filename=app.jfr     (on a running process)
jfr print --events jdk.GarbageCollection app.jfr
```

Open the recording in **JDK Mission Control** to see the hottest methods, the allocation
sources and the pauses. Other tools from the JDK:

| Tool | Answers |
| --- | --- |
| `jcmd <pid> GC.heap_info` | how full is the heap? |
| `jcmd <pid> Thread.print` | what is every thread doing right now? |
| `jcmd <pid> GC.heap_dump file.hprof` | what is in the heap? (open with Eclipse MAT or VisualVM) |
| `jstat -gcutil <pid> 1s` | GC activity, every second |
| `-Xlog:gc*:file=gc.log` | a log of every collection |

## A method for performance work

1. **Measure** with real load: latency percentiles (p50, p99), throughput, memory.
2. **Profile** to find where the time or the allocations go. It is rarely where you guessed.
3. **Fix the biggest cost**, often an algorithm, a missing index, a call in a loop, or a
   needless allocation, not a JVM flag.
4. **Measure again** and keep the change only if the numbers moved.

## Summary

- Objects live on the heap; stacks hold frames; metaspace holds classes.
- G1 is the default collector; ZGC trades some CPU and memory for sub-millisecond pauses.
- In containers, set `-XX:MaxRAMPercentage` and leave room outside the heap.
- The JIT optimises hot code after warm-up; use JMH for benchmarks and JFR to profile.
- Measure, profile, fix the biggest cost, measure again.

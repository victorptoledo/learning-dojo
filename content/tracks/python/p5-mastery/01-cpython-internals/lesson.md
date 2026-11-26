# CPython Internals: Bytecode, Memory and the Interpreter

You can write good Python without reading CPython's C code, but a mental model of what the
interpreter does explains performance, memory behaviour, the GIL and many "why does this
happen?" questions in interviews. This lesson is that model.

## From source to bytecode

```text
source text ─▶ tokens ─▶ AST ─▶ bytecode (code objects) ─▶ the evaluation loop
```

```python
import ast
import dis

print(ast.dump(ast.parse("total = price * qty"), indent=2))   # the syntax tree

def total(price, qty):
    return price * qty

dis.dis(total)
#  RESUME          0
#  LOAD_FAST_LOAD_FAST  1 (price, qty)
#  BINARY_OP       5 (*)
#  RETURN_VALUE
```

(Exact opcodes change between versions; that is why `.pyc` files are versioned.)

- The **compiler** turns each module, function and class body into a **code object**:
  bytecode plus constants, names and line tables (`total.__code__`).
- Imported modules are cached as **`.pyc`** files in `__pycache__`, tagged with the version
  (`model.cpython-315.pyc`), so the next start skips compilation.
- The **evaluation loop** (in `Python/ceval.c`) executes bytecode one instruction at a time,
  on a stack, inside a **frame** per function call. Since 3.11 frames are cheap and the loop
  **specialises** hot instructions for the types it sees (`BINARY_OP` becomes an int-only
  version, for example); `dis.dis(f, adaptive=True)` shows the specialised forms after warm-up.
- The experimental **JIT** (added in 3.13, much improved in 3.15) translates hot specialised traces into machine code by stitching
  pre-compiled templates ("copy and patch").

## Objects in C

Every Python object starts with the same header: a **reference count** and a pointer to its
**type**. A `list` adds a pointer to an array of object pointers; an `int` stores its digits.
That header is why even a small int costs 28 bytes, and why a list of a million ints costs far
more than a C array (use `array` or NumPy for dense numbers).

Some objects are shared as an optimisation:

```python
a = 256; b = 256
a is b          # True: ints from -5 to 256 are cached
a = 257; b = 257
a is b          # may be False: two objects (or True, if the compiler merged the constants)
```

Short identifier-like strings are **interned** (one shared object), and `sys.intern(s)` does
it on demand. All of this is an implementation detail: **never use `is` to compare values.**

Since 3.12, some objects are **immortal** (PEP 683): `None`, `True`, small ints and interned
strings have a reference count that never changes, which saves work and matters for
free-threading.

## Memory management

**Reference counting** is the main mechanism: each new reference increments the count, each
dropped one decrements it, and the object is freed **immediately** when it reaches zero. That
makes cleanup deterministic (a file closes when its last reference goes) but cannot free
**cycles**:

```python
a = {}; b = {"a": a}; a["b"] = b    # a and b reference each other
del a, b                            # counts stay at 1: only the cycle collector frees them
```

The **cyclic garbage collector** (`gc` module) periodically looks for groups of objects that
reference only each other. It is generational: new objects are scanned often, survivors less
often. `gc.collect()` forces a run; `gc.freeze()` before forking workers avoids copy-on-write
page faults caused by the collector touching shared objects. (3.14.0 to 3.14.4 shipped an
incremental collector; it was reverted because of memory pressure in production, and 3.15
uses the generational one.)

Small objects are allocated by **pymalloc** from arenas. Freed memory is kept for reuse, so a
process often does not shrink after you delete a big structure, even though nothing leaked.
To find real leaks, compare two `tracemalloc` snapshots taken some time apart, or use memray;
`gc.get_referrers(obj)` shows what keeps an object alive.

## The GIL, inside

On the standard build, a thread must hold the GIL to run bytecode. A thread that waits for I/O
releases it; a thread that runs Python code is asked to release it every **switch interval**
(`sys.getswitchinterval()`, 5 ms by default) so others get a turn. C extensions release it
around long native work.

The **free-threaded build** (3.13t to 3.15t) removes the GIL, which required changing the parts
the GIL used to protect:

| Problem without the GIL | Free-threaded solution |
| --- | --- |
| Every refcount change would need an atomic operation | **Biased reference counting**: the owning thread updates a cheap local count, other threads a shared atomic one; **deferred** counting and **immortal** objects avoid most updates on hot shared objects |
| Built-in containers (list, dict) could be corrupted by concurrent writes | **Per-object locks** inside the containers' C code |
| The allocator was not thread-safe | **mimalloc**, a thread-safe allocator |
| C extensions assume the GIL | Extensions must opt in; importing one that does not re-enables the GIL (and warns) |

That is also why the free-threaded build was slower for single-threaded code at first: the
specialising interpreter had to be made thread-safe, which 3.14 largely did.

## Subinterpreters

One process can host several interpreters, each with its own modules and, since 3.12, its
**own GIL** (PEP 684). 3.14 exposes them in the standard library as `concurrent.interpreters`
(PEP 734), with an `InterpreterPoolExecutor`. They give parallelism with isolation (no shared
objects, data passed by copying), somewhere between threads and processes.

## The C API and ABI

Extensions are written against CPython's C API. A wheel built for 3.14 normally does not load
on 3.15, because internal structures change; that is why packages publish one wheel per
version and platform. The **stable ABI** (`abi3` wheels, a limited subset of the API) builds
once for many versions; 3.15 adds one for free-threaded builds too (PEP 803). 3.15 also builds
CPython with **frame pointers** by default (PEP 831), so system profilers such as `perf` and
eBPF tools can unwind mixed Python and C stacks. PyO3 and Cython generate this glue for you. Tools such as debuggers,
profilers and coverage use **`sys.monitoring`** (PEP 669, 3.12), a low-overhead event API that
replaced the slow `sys.settrace` hooks for most uses.

## Summary

- Source → AST → bytecode in code objects → a specialising evaluation loop (and an
  experimental JIT); `.pyc` caches compiled modules; `dis` shows the bytecode.
- Objects have a refcount and a type; small ints and some strings are shared; `is` is not
  equality.
- Reference counting frees most objects at once; the generational cycle collector frees
  cycles; pymalloc keeps memory for reuse.
- The GIL switches every 5 ms; the free-threaded build replaces it with biased and deferred
  refcounts, per-object locks and mimalloc.
- Subinterpreters (own GIL) and the stable ABI complete the picture.

## Further reading

- `dis`, disassembler for Python bytecode: <https://docs.python.org/3/library/dis.html>
- `gc`, garbage collector interface: <https://docs.python.org/3/library/gc.html>
- The CPython internals documentation ("InternalDocs"): <https://github.com/python/cpython/tree/main/InternalDocs>
- PEP 683, immortal objects: <https://peps.python.org/pep-0683/>
- PEP 684, a per-interpreter GIL: <https://peps.python.org/pep-0684/>
- PEP 734, multiple interpreters in the standard library: <https://peps.python.org/pep-0734/>
- PEP 703, making the GIL optional (design of the free-threaded build): <https://peps.python.org/pep-0703/>

# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a compiler, in Java, from a small language with
   variables, `while`, `if`/`else` and `print` to bytecode for a stack machine, and the virtual
   machine that runs it. The hidden tests compare your exact bytecode, so the shape of the
   generated code matters, not only the output.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Virtual Memory, Paging and the TLB | How is an address translated? Why is RSS, not VSZ, what counts? |
| Page Table (kata) | When must a TLB entry be invalidated? |
| File Systems, Storage and I/O | What does `write` guarantee? How do you replace a file safely? |
| Networking Fundamentals | How many round trips does a fresh HTTPS request take, and why? |
| Compilers and Interpreters | How does a grammar encode precedence? Why is stack code post-order? |
| Expression Parser (kata) | How does one method per rule become a parser? |

> [!TIP]
> Get expressions right first, with `print` only, and check them against the bytecode table in
> the README. Then add variables, then `while`, then `if`. The jumps are easiest with
> **backpatching**: emit the jump with a dummy target, remember its index, and fix it once you
> know where the block ends.

# The CPU: Instruction Sets, Execution and Pipelines

Your program, after compilation or inside an interpreter, becomes a sequence of very simple
**instructions**: load a word, add two registers, compare, jump. A CPU executes billions of them a
second, and almost all of its complexity exists to make that sequence run faster than one
instruction per cycle while still *looking* sequential.

## The programmer's model: the ISA

The **instruction set architecture** is what a compiler sees:

- A few dozen **registers**: tiny, very fast storage inside the CPU. x86-64 has 16 general
  registers (`rax`, `rbx`, … `r15`), ARM64 and RISC-V have 31.
- A **program counter** (PC, `rip` on x86-64): the address of the next instruction.
- **Memory**, a large array of bytes addressed by number.
- **Instructions**: arithmetic and logic (`add`, `and`, `shl`), data movement (`load`, `store`,
  `mov`), control flow (`jmp`, conditional branches, `call`, `ret`), and a few special ones
  (system calls, atomic operations, fences).

A sum loop in RISC-V assembly:

```text
# a0 = address of the array, a1 = count; result in a2
        li   a2, 0            # total = 0
loop:   beqz a1, done         # if count == 0 goto done
        lw   t0, 0(a0)        # t0 = memory[a0]      (load word)
        add  a2, a2, t0       # total += t0
        addi a0, a0, 4        # next element (4 bytes)
        addi a1, a1, -1       # count--
        j    loop
done:
```

### RISC and CISC

- **RISC** (ARM, RISC-V): fixed-length instructions, only loads and stores touch memory
  (**load-store architecture**), many registers. Simple to decode and pipeline.
- **CISC** (x86-64): variable-length instructions (1 to 15 bytes), arithmetic can read memory
  directly. Modern x86 cores translate each instruction into RISC-like **micro-operations**
  internally, so the distinction lives mostly in the decoder.

An instruction is itself bits: an **opcode** that says what to do and fields that name registers
or hold an **immediate** constant. In the checkpoint kata you decode such words yourself.

## Fetch, decode, execute

The basic cycle, repeated forever:

1. **Fetch** the instruction at PC from memory (through the instruction cache).
2. **Decode** it: which operation, which registers, which immediate.
3. **Execute** it in the ALU (or compute an address).
4. **Memory**: for loads and stores, access the data cache.
5. **Write back** the result to a register, and set PC to the next instruction (or the branch
   target).

## Functions, the stack and calling conventions

There is no "function" in hardware, only `call` (push the return address and jump) and `ret`
(pop it and jump back). Everything else is a **calling convention**, an agreement between
compilers:

- The first arguments travel in registers (`rdi`, `rsi`, `rdx`, … on Linux x86-64; `a0`–`a7` on
  RISC-V), the result comes back in `rax` / `a0`.
- Each call gets a **stack frame** on the **call stack**, a region of memory that grows down: the
  return address, saved registers and local variables that do not fit in registers.
- Some registers are **caller-saved** (the callee may destroy them), others **callee-saved**.

That is where a `StackOverflowError` or `RecursionError` comes from: the stack has a fixed size
(often 1–8 MiB per thread), and deep recursion runs out of frames.

## Pipelining

Executing one instruction at a time wastes most of the circuit. A **pipeline** overlaps them like
an assembly line: while one instruction executes, the next is being decoded and the one after is
being fetched.

```text
cycle:     1   2   3   4   5   6   7
instr 1    F   D   E   M   W
instr 2        F   D   E   M   W
instr 3            F   D   E   M   W
```

Once full, a five-stage pipeline finishes one instruction per cycle. Modern cores have 10–20
stages. Pipelines have **hazards**:

- **Data hazard**: an instruction needs a result that is not written yet. Solved by
  **forwarding** results between stages, or by stalling a cycle (for example after a load).
- **Control hazard**: after a branch, which instruction comes next? The CPU cannot wait.
- **Structural hazard**: two instructions need the same unit at once.

## Branch prediction and speculation

The CPU **predicts** each branch from its history and keeps fetching and executing along the
guessed path (**speculative execution**). A correct guess costs nothing; a **misprediction**
throws away the speculative work, typically 10–20 cycles. Predictors are right more than 95% of
the time on typical code, but a branch on random data is a coin flip. That is why:

- Sorting data before a filtering loop can make it several times faster.
- Branch-free code (conditional moves, masks, `min`/`max`) wins on unpredictable conditions.
- Tight loops with predictable exits cost almost nothing per branch.

Speculation also has a dark side: **Spectre** and **Meltdown** (2018) showed that speculative
work leaves traces in the caches that an attacker can measure.

## Doing more than one thing per cycle

- **Superscalar**: several pipelines; a modern core can start 4–8 instructions per cycle.
- **Out-of-order execution**: the core looks ahead through a window of hundreds of instructions,
  runs those whose inputs are ready, and **retires** them in program order so the result looks
  sequential. A cache miss on one load no longer stops the instructions that do not depend on it.
- **SIMD** (vector) instructions apply one operation to 4, 8 or 16 values at once (SSE, AVX,
  NEON). Compilers and JITs **auto-vectorise** simple loops over arrays.
- **Multiple cores**: independent processors on one chip, sharing the last-level cache and memory.
  **Simultaneous multithreading** (Hyper-Threading) runs two threads on one core's units.

Clock frequencies stopped rising around 2005 (power and heat), so since then performance comes
from more work per cycle and more cores. Single-threaded code does not get faster by itself any
more: that is the "free lunch is over" argument for concurrency.

## Summary

- The ISA is registers, a program counter, memory and simple instructions; RISC and CISC differ
  mostly in decoding.
- Function calls are a convention over `call`, `ret`, registers and the stack.
- Pipelines overlap instructions; hazards are handled with forwarding, stalls and prediction.
- A mispredicted branch costs 10–20 cycles; predictable branches are almost free.
- Superscalar, out-of-order, SIMD and multiple cores are where modern performance comes from.

## Further reading

- Patterson and Hennessy, *Computer Organization and Design: The Hardware/Software Interface, RISC-V Edition*, chapters 2 "Instructions" and 4 "The Processor"
- Bryant and O'Hallaron, *Computer Systems: A Programmer's Perspective*, 3rd ed., chapters 3 "Machine-Level Representation of Programs" and 4 "Processor Architecture"
- Hennessy and Patterson, *Computer Architecture: A Quantitative Approach*, 6th ed., chapter 3 (instruction-level parallelism)
- Herb Sutter, "The Free Lunch Is Over": <http://www.gotw.ca/publications/concurrency-ddj.htm>
- The RISC-V Instruction Set Manual, Volume I: <https://riscv.org/specifications/>
- Stack Overflow, "Why is processing a sorted array faster than processing an unsorted array?": <https://stackoverflow.com/questions/11227809>

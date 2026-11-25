# Model Checker

Testing a concurrent program runs a few interleavings and hopes. A **model checker** runs
*all* of them: it treats the program as a finite automaton whose states are snapshots of the
whole machine and whose transitions are single atomic steps of one thread, explores every
reachable state once, and reports the shortest schedule that breaks a rule. This is how
TLA+, SPIN and Java PathFinder find the race that shows up once a month in production.

Write `checker.py`. A program is a list of threads; a thread is a list of instructions
(tuples). Shared variables start with the values you are given; registers are private to a
thread and start at 0; locks start free.

## The instructions

An *operand* is either an `int` or the name of one of the thread's registers.

| Instruction | Effect (one atomic step) |
| --- | --- |
| `("load", reg, var)` | `reg = var` |
| `("store", var, operand)` | `var = operand` |
| `("set", reg, operand)` | `reg = operand` |
| `("add", reg, operand)` | `reg = reg + operand` |
| `("cas", reg, var, expected, new)` | if `var == expected`: `var = new` and `reg = 1`; else `reg = 0` |
| `("lock", name)` | takes the lock; the thread **cannot run** while another thread holds it |
| `("unlock", name)` | releases the lock; releasing a lock the thread does not hold raises `ProgramError` |
| `("jump", target)` | continues at instruction `target` |
| `("jump_if", reg, cmp, operand, target)` | if `reg cmp operand` (`==`, `!=`, `<`, `>=`) continues at `target`, else at the next instruction |
| `("critical",)` | does nothing; marks the critical section |

A thread whose program counter equals the length of its code has **finished**. Every other
instruction moves the program counter to the next one. Note that `count++` really is three
instructions: `load`, `add` and `store`.

## What to write

`State` and `Report` are given. A `State` keeps its pairs sorted by name, so two equal
snapshots are equal (and hash the same), whatever order the values were written in.

- `validate(threads, shared)` raises `ValueError` naming the thread and the instruction
  (`"thread 1, instruction 3: ..."`) for an unknown instruction, a wrong number of operands,
  a `load`, `store` or `cas` on a variable that is not in `shared`, an unknown comparison,
  or a jump target outside `0..len(thread)` (jumping to `len(thread)` finishes the thread).
- `initial_state(threads, shared)`, `enabled(threads, state)` (the threads that are neither
  finished nor waiting for a lock, in increasing order), `step(threads, state, thread)` (the
  next state, without changing the given one) and `is_final(threads, state)`.
- `replay(threads, shared, schedule)` runs a list of thread numbers from the initial state and
  raises `ValueError` when one of them is not enabled at its turn.
- `mutual_exclusion(threads)` returns an invariant: a function from a state to `True` when at
  most one thread is at a `critical` instruction.
- `explore(threads, shared, invariant=None, max_states=100_000)` validates the program, then
  visits every reachable state exactly once and returns a `Report`:
  - `states`: how many distinct states are reachable (the initial one included);
  - `finals`: the reachable states where every thread has finished;
  - `violation`: a **shortest** schedule that reaches a state breaking the invariant, or whose
    last step raises `ProgramError`; `None` if there is none;
  - `deadlock`: a **shortest** schedule that reaches a state where some thread has not
    finished and no thread can run; `None` if there is none.
  Exploring never stops at a violation. When more than `max_states` states are reachable it
  raises `StateSpaceTooLarge` instead of running out of memory.

## What the grading tests check

- Every instruction, `validate`'s messages, `enabled`, `step` and `replay`.
- The lost update of `count++`, the same counter behind a lock and with a `cas` loop.
- Deadlock with two locks taken in opposite orders, and the fix by lock ordering.
- A naive "check a flag, then set it" lock that breaks mutual exclusion, and Peterson's
  algorithm, which keeps it with no deadlock.
- That every schedule reported is as short as possible and really reproduces the problem.
- Exact state counts (repeated states are merged), a program 12,000 states deep (no
  recursion), a state space of about 16,000 states explored in a few seconds, and
  `max_states`.
- The structure: no function longer than 30 lines, no recursion, no `global`, and `State`
  still a frozen dataclass.

> Tip: breadth-first search gives shortest schedules for free if you remember, for every new
> state, the state and the thread it came from, and check each state the moment you first
> reach it. Rebuild the schedule by walking those parents back to the start.

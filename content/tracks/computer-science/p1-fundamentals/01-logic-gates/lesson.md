# Boolean Logic, Gates and Circuits

The previous phase showed that everything is bits. This lesson shows how bits are **computed**:
with a handful of logic gates, wired so that adding two 64-bit numbers is just electricity
settling. You will not design hardware, but knowing the shape of it explains why some operations
are cheap, why clocks have a frequency, and why shifting is faster than dividing.

## Boolean algebra

A Boolean variable is 0 or 1. Three operations are enough for everything:

| A | B | A AND B | A OR B | NOT A | A XOR B |
| --- | --- | --- | --- | --- | --- |
| 0 | 0 | 0 | 0 | 1 | 0 |
| 0 | 1 | 0 | 1 | 1 | 1 |
| 1 | 0 | 0 | 1 | 0 | 1 |
| 1 | 1 | 1 | 1 | 0 | 0 |

Useful laws, the same ones you use to simplify `if` conditions:

- **De Morgan**: `NOT (A AND B) = NOT A OR NOT B` and `NOT (A OR B) = NOT A AND NOT B`.
- Distribution: `A AND (B OR C) = (A AND B) OR (A AND C)`.
- Absorption: `A OR (A AND B) = A`.
- `A XOR B = (A AND NOT B) OR (NOT A AND B)`; XOR is "different", XNOR is "equal".

Any truth table can be written as an OR of ANDs (**sum of products**): one AND term per row whose
output is 1. So any function of bits can be built from AND, OR and NOT.

## Gates and transistors

A **transistor** is a voltage-controlled switch. In CMOS, two or four transistors make a NAND or
NOR gate; other gates are built from those. **NAND is universal**: `NOT A = A NAND A`, `A AND B =
NOT (A NAND B)`, and by De Morgan `A OR B = (NOT A) NAND (NOT B)`. Nand to Tetris builds a whole
computer from NAND gates alone.

Real gates are not instant: each has a **propagation delay** of a few picoseconds. The longest
chain of gates between an input and an output, the **critical path**, decides how fast a circuit
can run.

## Combinational circuits: adders and multiplexers

A **combinational** circuit has no memory: its outputs depend only on its current inputs.

**Half adder** (two bits): `sum = A XOR B`, `carry = A AND B`.

**Full adder** (two bits and a carry in):

```text
sum   = A XOR B XOR Cin
Cout  = (A AND B) OR (Cin AND (A XOR B))
```

Chain *n* full adders, each passing its carry to the next, and you get a **ripple-carry adder**
for *n*-bit numbers. It is correct but slow: bit 63 waits for the carry from bit 0. Real CPUs use
**carry-lookahead** or prefix adders that compute all carries in parallel in O(log n) gate
delays.

Subtraction comes free with two's complement: `A − B = A + NOT B + 1`, so the same adder with
inverted inputs and a carry in of 1 subtracts.

A **multiplexer** (mux) selects one of several inputs with a control signal:
`out = (NOT s AND x) OR (s AND y)`. An **ALU** (arithmetic logic unit) computes several results at
once (add, and, or, xor, shift) and a mux picks the one the instruction asked for. A **decoder**
turns an *n*-bit number into one of 2ⁿ output lines, which is how a memory address selects a row.

## Why some operations are cheap

- AND, OR, XOR, NOT: one gate per bit, all bits in parallel: one cycle.
- Shifts by a constant are just wires; a **barrel shifter** shifts by any amount in log₂(n)
  stages.
- Addition: a fast adder, one cycle.
- Multiplication: an array of adders, a few cycles.
- Division: an iterative circuit, tens of cycles. Compilers replace `x / 8` with `x >> 3` (with a
  fix-up for negative numbers) and division by other constants with a multiplication.

## Sequential circuits: state and the clock

To remember anything, a circuit needs **feedback**. Two cross-coupled NOR gates form an **SR
latch** that holds one bit. A **D flip-flop** stores its input D at the moment the **clock**
signal rises and holds it until the next rising edge.

- A **register** is a row of flip-flops sharing a clock: 64 of them hold one 64-bit value.
- A **sequential** circuit is combinational logic between registers. On each clock edge, the
  registers capture the results computed since the previous edge.
- The **clock period** must be longer than the critical path, otherwise registers capture a value
  that has not settled. A 4 GHz clock leaves 250 ps per cycle.
- **SRAM** (caches, registers) stores a bit in six transistors; **DRAM** (main memory) in one
  transistor and a capacitor that leaks and must be refreshed. That is why caches are fast and
  small and RAM is big and slow.

A **finite state machine** in hardware is exactly this: a register that holds the current state
and combinational logic that computes the next state from the state and the inputs. You will meet
the same idea in theory, as the automata of phase 3.

## Summary

- Boolean algebra (AND, OR, NOT, XOR and De Morgan) describes every digital circuit; NAND alone is
  enough.
- Adders, multiplexers and decoders are combinational; an ALU is several of them behind a mux.
- Gate delays create a critical path that limits the clock frequency.
- Flip-flops and a clock add state: registers, SRAM, and finite state machines.

## Further reading

- Nisan and Schocken, *The Elements of Computing Systems* (Nand to Tetris), 2nd ed., chapters 1–3 "Boolean Logic", "Boolean Arithmetic", "Memory": <https://www.nand2tetris.org/>
- Harris and Harris, *Digital Design and Computer Architecture: RISC-V Edition*, chapters 1–3
- Patt and Patel, *Introduction to Computing Systems*, 3rd ed., chapter 3 "Digital Logic Structures"
- Petzold, *Code: The Hidden Language of Computer Hardware and Software*, 2nd ed., chapters on logic gates and adding machines
- MIT OCW 6.004 Computation Structures (Spring 2017): <https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/>

# Toy Machine

Build a small computer in `machine.py`: decode 16-bit instruction words, execute them on eight
16-bit registers, and count the cycles the program takes, with a direct-mapped data cache
deciding how expensive each load and store is. It combines the whole phase: two's complement,
instruction formats and branches, and caches. `OPS`, `Instruction`, `Result` and `MachineError`
are already declared.

## The instruction set

Every instruction is one 16-bit word. The top 4 bits are the **opcode**; registers are 3-bit
fields; immediates are **signed two's complement**.

```text
 15  12 11   9 8    6 5   3 2   0
| op   |  rd  |  rs  |  rt  |  -  |   add sub and or slt
| op   |  rd  |  rs  |   imm6      |   addi load store beq bne
| op   |  rd  |      imm9          |   li
| op   |          imm12            |   jmp
| 0000 |        (ignored)          |   halt
```

| Opcode | Instruction | Meaning |
| --- | --- | --- |
| 0 | `halt` | stop |
| 1–4 | `add` `sub` `and` `or` rd, rs, rt | rd = rs op rt |
| 5 | `slt rd, rs, rt` | rd = 1 if rs < rt (signed) else 0 |
| 6 | `addi rd, rs, imm` | rd = rs + imm (imm from −32 to 31) |
| 7 | `li rd, imm` | rd = imm (imm from −256 to 255) |
| 8 | `load rd, [rs+imm]` | rd = memory[rs + imm] |
| 9 | `store rd, [rs+imm]` | memory[rs + imm] = rd |
| 10 | `beq rd, rs, imm` | if rd == rs: pc = pc + 1 + imm |
| 11 | `bne rd, rs, imm` | if rd != rs: pc = pc + 1 + imm |
| 12 | `jmp imm` | pc = pc + 1 + imm (imm from −2048 to 2047) |
| 13–15 | — | invalid |

## What to write

**`decode(word) -> Instruction`**: the fields of a word. Fields an instruction does not use stay
0 (`halt` is always `Instruction("halt")`). `ValueError` for a word outside 0..0xFFFF or an
invalid opcode.

**`disassemble(word) -> str`**, in exactly these forms:

```text
halt     add r1, r2, r3     addi r1, r0, -3     li r3, -200
load r2, [r1+4]     store r2, [r1-32]     beq r1, r2, -3     jmp -5
```

**`run(program, memory=None, *, cache_lines=4, line_words=4, max_steps=100_000) -> Result`**:

- Decode the whole program first (an invalid word raises `ValueError` before anything runs).
  Execution starts at pc 0 with all registers 0.
- **Registers** hold signed 16-bit values; results wrap (32767 + 1 = −32768). **r0 always
  reads 0**: writes to it are ignored.
- **Memory** is 65,536 words, all 0 except those in `memory` (a dict address → value; wrap the
  values to signed 16 bits). Addresses `rs + imm` wrap around the 16-bit space.
- `Result.memory` holds the **non-zero** words at the end, sorted by address. `Result.trace`
  lists the pc of every instruction executed, including `halt`.
- `MachineError` if the pc leaves the program or the program has not halted after `max_steps`
  instructions.

**Cycles and the cache.** Every instruction costs 1 cycle. A taken branch or `jmp` costs 2 more
(the pipeline is flushed). Every `load` and `store` goes through a **direct-mapped** cache of
`cache_lines` lines of `line_words` words (both powers of two, otherwise `ValueError`): word
address `a` is in line `a // line_words`, which can only sit in slot `line % cache_lines`. A hit
costs 1 extra cycle, a miss 10 extra cycles and loads the line into its slot (stores allocate
too). Count `cache_hits`, `cache_misses`, `cycles` and `instructions`.

## What the hidden tests check

Decoding and disassembly of every format and sign; an assembler in the tests then runs real
programs on your machine: an array sum (with its exact cycle count), wrap-around, r0, signed
comparisons, memory and address wrapping, branch penalties, cache conflicts, errors, a factorial
and a bubble sort.

Run the visible tests with `uv run pytest`.

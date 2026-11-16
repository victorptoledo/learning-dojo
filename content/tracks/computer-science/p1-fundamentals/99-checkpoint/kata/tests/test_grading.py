import random

import pytest

from machine import Instruction, MachineError, decode, disassemble, run

OPCODES = {op: n for n, op in enumerate(
    ["halt", "add", "sub", "and", "or", "slt", "addi", "li", "load", "store", "beq", "bne", "jmp"]
)}


def assemble(source: str) -> list[int]:
    """A small assembler for the tests: one instruction per line, `label:` lines, `#` comments."""
    lines = [line.split("#")[0].strip() for line in source.splitlines()]
    lines = [line for line in lines if line]
    labels, code = {}, []
    for line in lines:
        if line.endswith(":"):
            labels[line[:-1]] = len(code)
        else:
            code.append(line)

    def reg(text):
        return int(text.strip().lstrip("r"))

    def imm(text, pc, bits):
        text = text.strip()
        value = labels[text] - (pc + 1) if text in labels else int(text)
        assert -(1 << (bits - 1)) <= value < 1 << (bits - 1), f"{text} does not fit"
        return value & ((1 << bits) - 1)

    words = []
    for pc, line in enumerate(code):
        op, _, rest = line.partition(" ")
        args = [a for a in rest.replace("[", "").replace("]", "").replace("+", ",+").replace("-", ",-").split(",") if a.strip()]
        word = OPCODES[op] << 12
        if op in ("add", "sub", "and", "or", "slt"):
            word |= reg(args[0]) << 9 | reg(args[1]) << 6 | reg(args[2]) << 3
        elif op == "li":
            word |= reg(args[0]) << 9 | imm(args[1], pc, 9)
        elif op == "jmp":
            word |= imm(args[0], pc, 12)
        elif op != "halt":
            offset = args[2] if len(args) > 2 else "0"
            word |= reg(args[0]) << 9 | reg(args[1]) << 6 | imm(offset, pc, 6)
        words.append(word)
    return words


# ---- decode and disassemble -------------------------------------------------------------

@pytest.mark.parametrize(
    "word, instruction, text",
    [
        (0x0000, Instruction("halt"), "halt"),
        (0x1298, Instruction("add", rd=1, rs=2, rt=3), "add r1, r2, r3"),
        (0x2E3F, Instruction("sub", rd=7, rs=0, rt=7), "sub r7, r0, r7"),
        (0x5650, Instruction("slt", rd=3, rs=1, rt=2), "slt r3, r1, r2"),
        (0x623D, Instruction("addi", rd=1, rs=0, imm=-3), "addi r1, r0, -3"),
        (0x621F, Instruction("addi", rd=1, rs=0, imm=31), "addi r1, r0, 31"),
        (0x7738, Instruction("li", rd=3, imm=-200), "li r3, -200"),
        (0x76FF, Instruction("li", rd=3, imm=255), "li r3, 255"),
        (0x8444, Instruction("load", rd=2, rs=1, imm=4), "load r2, [r1+4]"),
        (0x9460, Instruction("store", rd=2, rs=1, imm=-32), "store r2, [r1-32]"),
        (0x9040, Instruction("store", rd=0, rs=1, imm=0), "store r0, [r1+0]"),
        (0xA2BD, Instruction("beq", rd=1, rs=2, imm=-3), "beq r1, r2, -3"),
        (0xB005, Instruction("bne", rd=0, rs=0, imm=5), "bne r0, r0, 5"),
        (0xCFFB, Instruction("jmp", imm=-5), "jmp -5"),
        (0xC7FF, Instruction("jmp", imm=2047), "jmp 2047"),
    ],
)
def test_decode_and_disassemble(word, instruction, text):
    assert decode(word) == instruction
    assert disassemble(word) == text


def test_halt_ignores_the_other_bits_and_unused_fields_are_zero():
    assert decode(0x0FFF) == Instruction("halt")
    assert decode(0x1297) == Instruction("add", rd=1, rs=2, rt=2)
    assert decode(0x7FFF) == Instruction("li", rd=7, imm=-1)


def test_decode_rejects_bad_words():
    for word in [0xD000, 0xE123, 0xFFFF, -1, 0x10000]:
        with pytest.raises(ValueError):
            decode(word)


def test_decode_matches_the_assembler_on_random_instructions():
    rng = random.Random(16)
    for _ in range(500):
        rd, rs, rt = rng.randrange(8), rng.randrange(8), rng.randrange(8)
        k = rng.randint(-32, 31)
        line = rng.choice([
            f"add r{rd}, r{rs}, r{rt}", f"or r{rd}, r{rs}, r{rt}", f"addi r{rd}, r{rs}, {k}",
            f"load r{rd}, [r{rs}{k:+d}]", f"bne r{rd}, r{rs}, {k}",
        ])
        word = assemble(line)[0]
        assert disassemble(word) == line


# ---- running programs -------------------------------------------------------------------

SUM = """
        li r1, 100        # pointer to the array
        li r2, 10         # count
        li r3, 0          # sum
loop:
        beq r2, r0, done
        load r4, [r1+0]
        add r3, r3, r4
        addi r1, r1, 1
        addi r2, r2, -1
        jmp loop
done:
        store r3, [r0+31]
        halt
"""


def test_sum_of_an_array():
    data = {100 + i: v for i, v in enumerate([5, -3, 12, 7, 0, 1, 1, 2, 3, 100])}
    result = run(assemble(SUM), data)
    assert result.registers[3] == 128
    assert result.memory[31] == 128
    assert result.registers[1] == 110 and result.registers[2] == 0


def test_sum_counts_instructions_cycles_and_cache_use():
    data = {100 + i: 1 for i in range(10)}
    result = run(assemble(SUM), data, cache_lines=4, line_words=4)
    assert result.instructions == 66
    # 10 loads over lines 25, 26, 27: 3 misses and 7 hits; the store to word 31 misses
    assert (result.cache_misses, result.cache_hits) == (4, 7)
    # 66 base cycles + 11 taken branches × 2 + 4 misses × 10 + 7 hits × 1
    assert result.cycles == 66 + 22 + 40 + 7


def test_trace_lists_the_executed_pcs():
    result = run(assemble("li r1, 1\nbne r1, r0, end\nli r2, 9\nend:\nhalt"))
    assert result.trace == [0, 1, 3]
    assert result.registers[2] == 0


def test_r0_is_always_zero():
    result = run(assemble("li r0, 5\naddi r1, r0, 1\nadd r0, r1, r1\nhalt"))
    assert result.registers[0] == 0
    assert result.registers[1] == 1


def test_arithmetic_wraps_at_16_bits():
    doubling = "li r1, 1\n" + "add r1, r1, r1\n" * 15 + "add r2, r1, r1\naddi r3, r1, -1\nhalt"
    regs = run(assemble(doubling)).registers
    assert regs[1] == -32768
    assert regs[2] == 0
    assert regs[3] == 32767


def test_signed_comparisons_and_logic():
    program = """
        li r1, -1
        li r2, 1
        slt r3, r1, r2
        slt r4, r2, r1
        and r5, r1, r2
        or r6, r0, r1
        sub r7, r2, r1
        halt
    """
    regs = run(assemble(program)).registers
    assert regs[3:] == [1, 0, 1, -1, 2]


def test_memory_values_wrap_and_only_non_zero_words_are_returned():
    program = "li r1, -7\nstore r1, [r0+3]\nload r2, [r0+5]\nstore r0, [r0+4]\nhalt"
    result = run(assemble(program), {4: 70000, 5: 0x8000})
    assert result.registers[2] == -32768
    assert result.memory == {3: -7, 5: -32768}
    assert list(result.memory) == sorted(result.memory)


def test_addresses_wrap_around_the_16_bit_space():
    result = run(assemble("li r1, -1\nstore r1, [r1+2]\nhalt"))
    assert result.memory == {1: -1}


def test_taken_branches_cost_two_extra_cycles():
    straight = run(assemble("li r1, 1\nbeq r1, r0, 0\nhalt"))
    taken = run(assemble("li r1, 1\nbne r1, r0, 0\nhalt"))
    assert straight.cycles == 3
    assert taken.cycles == 5
    assert run(assemble("jmp 0\nhalt")).cycles == 4


def test_conflicting_addresses_miss_in_a_small_cache():
    program = """
        li r1, 10
loop:
        load r2, [r0+0]
        load r3, [r0+16]
        addi r1, r1, -1
        bne r1, r0, loop
        halt
    """
    small = run(assemble(program), cache_lines=4, line_words=4)
    assert (small.cache_hits, small.cache_misses) == (0, 20)
    big = run(assemble(program), cache_lines=8, line_words=4)
    assert (big.cache_hits, big.cache_misses) == (18, 2)
    assert small.cycles - big.cycles == 18 * 9


def test_cache_shape_is_validated():
    for lines, words in [(3, 4), (4, 0), (0, 4)]:
        with pytest.raises(ValueError):
            run([0], cache_lines=lines, line_words=words)


def test_running_off_the_program_is_an_error():
    with pytest.raises(MachineError):
        run(assemble("li r1, 1"))
    with pytest.raises(MachineError):
        run(assemble("jmp -3\nhalt"))


def test_a_program_that_never_halts_is_stopped():
    with pytest.raises(MachineError):
        run(assemble("loop:\njmp loop\nhalt"), max_steps=1000)


def test_an_invalid_word_anywhere_is_rejected_before_running():
    with pytest.raises(ValueError):
        run([0x0000, 0xE000])


FACTORIAL = """
        li r1, 7            # n
        li r2, 1            # result
outer:
        beq r1, r0, done
        li r3, 0            # product = result * n by repeated addition
        or r4, r1, r0       # counter = n
inner:
        add r3, r3, r2
        addi r4, r4, -1
        bne r4, r0, inner
        or r2, r3, r0
        addi r1, r1, -1
        jmp outer
done:
        store r2, [r0+0]
        halt
"""


def test_factorial_by_repeated_addition():
    result = run(assemble(FACTORIAL))
    assert result.registers[2] == 5040
    assert result.memory == {0: 5040}
    assert result.cache_misses == 1


def test_bubble_sort_in_memory():
    program = """
        li r7, 8           # n
pass:
        li r6, 0           # swapped = false
        li r1, 64          # i = base
        addi r5, r7, -1    # comparisons left
step:
        beq r5, r0, check
        load r2, [r1+0]
        load r3, [r1+1]
        slt r4, r3, r2
        beq r4, r0, next
        store r3, [r1+0]
        store r2, [r1+1]
        li r6, 1
next:
        addi r1, r1, 1
        addi r5, r5, -1
        jmp step
check:
        bne r6, r0, pass
        halt
    """
    values = [9, -4, 300, 0, 7, -255, 12, 1]
    result = run(assemble(program), {64 + i: v for i, v in enumerate(values)})
    assert [result.memory.get(64 + i, 0) for i in range(8)] == sorted(values)

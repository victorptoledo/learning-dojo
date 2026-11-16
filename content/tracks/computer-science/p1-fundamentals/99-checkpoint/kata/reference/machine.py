"""A toy 16-bit load-store machine with a direct-mapped data cache and a cycle count."""

from dataclasses import dataclass, field

OPS = ["halt", "add", "sub", "and", "or", "slt", "addi", "li",
       "load", "store", "beq", "bne", "jmp"]
REGISTER_OPS = {"add", "sub", "and", "or", "slt"}


class MachineError(Exception):
    """The program did something the machine cannot do."""


@dataclass(frozen=True)
class Instruction:
    op: str
    rd: int = 0
    rs: int = 0
    rt: int = 0
    imm: int = 0


@dataclass
class Result:
    registers: list[int]
    memory: dict[int, int]
    cycles: int = 0
    instructions: int = 0
    cache_hits: int = 0
    cache_misses: int = 0
    trace: list[int] = field(default_factory=list)


def _signed(value: int, bits: int) -> int:
    value &= (1 << bits) - 1
    return value - (1 << bits) if value >> (bits - 1) else value


def decode(word: int) -> Instruction:
    """Splits a 16-bit instruction word into its fields."""
    if not 0 <= word <= 0xFFFF:
        raise ValueError(f"not a 16-bit word: {word}")
    opcode = word >> 12
    if opcode >= len(OPS):
        raise ValueError(f"invalid opcode {opcode} in 0x{word:04X}")
    op = OPS[opcode]
    rd, rs = word >> 9 & 7, word >> 6 & 7
    if op == "halt":
        return Instruction("halt")
    if op == "jmp":
        return Instruction("jmp", imm=_signed(word, 12))
    if op == "li":
        return Instruction("li", rd=rd, imm=_signed(word, 9))
    if op in REGISTER_OPS:
        return Instruction(op, rd=rd, rs=rs, rt=word >> 3 & 7)
    return Instruction(op, rd=rd, rs=rs, imm=_signed(word, 6))


def disassemble(word: int) -> str:
    """The assembly text of one instruction word."""
    i = decode(word)
    if i.op == "halt":
        return "halt"
    if i.op == "jmp":
        return f"jmp {i.imm}"
    if i.op == "li":
        return f"li r{i.rd}, {i.imm}"
    if i.op in REGISTER_OPS:
        return f"{i.op} r{i.rd}, r{i.rs}, r{i.rt}"
    if i.op in ("load", "store"):
        return f"{i.op} r{i.rd}, [r{i.rs}{i.imm:+d}]"
    return f"{i.op} r{i.rd}, r{i.rs}, {i.imm}"


def run(
    program: list[int],
    memory: dict[int, int] | None = None,
    *,
    cache_lines: int = 4,
    line_words: int = 4,
    max_steps: int = 100_000,
) -> Result:
    """Runs a program until halt; returns registers, memory, cycles and cache statistics."""
    for size in (cache_lines, line_words):
        if size < 1 or size & (size - 1):
            raise ValueError("cache_lines and line_words must be powers of two")
    decoded = [decode(word) for word in program]
    mem = {address & 0xFFFF: _signed(value, 16) for address, value in (memory or {}).items()}
    regs = [0] * 8
    tags: list[int | None] = [None] * cache_lines
    result = Result(regs, mem)
    pc = 0

    def write(register: int, value: int) -> None:
        if register:
            regs[register] = _signed(value, 16)

    def touch(address: int) -> None:
        line = address // line_words
        index, tag = line % cache_lines, line // cache_lines
        if tags[index] == tag:
            result.cache_hits += 1
            result.cycles += 1
        else:
            tags[index] = tag
            result.cache_misses += 1
            result.cycles += 10

    while True:
        if not 0 <= pc < len(decoded):
            raise MachineError(f"pc {pc} is outside the program")
        if result.instructions == max_steps:
            raise MachineError(f"no halt after {max_steps} instructions")
        i = decoded[pc]
        result.trace.append(pc)
        result.instructions += 1
        result.cycles += 1
        next_pc = pc + 1
        a, b = regs[i.rs], regs[i.rt]
        match i.op:
            case "halt":
                break
            case "add":
                write(i.rd, a + b)
            case "sub":
                write(i.rd, a - b)
            case "and":
                write(i.rd, a & b)
            case "or":
                write(i.rd, a | b)
            case "slt":
                write(i.rd, int(a < b))
            case "addi":
                write(i.rd, a + i.imm)
            case "li":
                write(i.rd, i.imm)
            case "load" | "store":
                address = (a + i.imm) & 0xFFFF
                touch(address)
                if i.op == "load":
                    write(i.rd, mem.get(address, 0))
                else:
                    mem[address] = regs[i.rd]
            case "beq" | "bne" | "jmp":
                taken = (
                    i.op == "jmp"
                    or (i.op == "beq" and regs[i.rd] == a)
                    or (i.op == "bne" and regs[i.rd] != a)
                )
                if taken:
                    next_pc = pc + 1 + i.imm
                    result.cycles += 2
        pc = next_pc
    result.memory = {address: value for address, value in sorted(mem.items()) if value}
    return result

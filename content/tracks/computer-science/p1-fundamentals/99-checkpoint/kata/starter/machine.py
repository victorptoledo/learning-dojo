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


def decode(word: int) -> Instruction:
    """Splits a 16-bit instruction word into its fields."""
    raise NotImplementedError("todo: write decode()")


def disassemble(word: int) -> str:
    """The assembly text of one instruction word."""
    raise NotImplementedError("todo: write disassemble()")


def run(
    program: list[int],
    memory: dict[int, int] | None = None,
    *,
    cache_lines: int = 4,
    line_words: int = 4,
    max_steps: int = 100_000,
) -> Result:
    """Runs a program until halt; returns registers, memory, cycles and cache statistics."""
    raise NotImplementedError("todo: write run()")

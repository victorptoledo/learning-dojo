"""A lexer, a recursive-descent parser and a tree-walking evaluator for arithmetic expressions."""

import math
from collections.abc import Callable
from dataclasses import dataclass

OPERATORS = "+-*/%^(),"


class ParseError(Exception):
    def __init__(self, message: str, pos: int):
        super().__init__(f"{message} at position {pos}")
        self.pos = pos


class EvalError(Exception):
    pass


@dataclass(frozen=True)
class Token:
    kind: str  # "number", "ident", "op" or "eof"
    text: str
    pos: int


@dataclass(frozen=True)
class Num:
    value: int | float


@dataclass(frozen=True)
class Var:
    name: str


@dataclass(frozen=True)
class Unary:
    op: str
    operand: "Node"


@dataclass(frozen=True)
class Binary:
    op: str
    left: "Node"
    right: "Node"


@dataclass(frozen=True)
class Call:
    name: str
    args: tuple["Node", ...]


type Node = Num | Var | Unary | Binary | Call


def tokenize(source: str) -> list[Token]:
    """The tokens of source, ending with an "eof" token at position len(source)."""
    raise NotImplementedError("todo: write tokenize()")


def parse(source: str) -> Node:
    """The AST of a whole expression; ParseError with a position on bad input."""
    raise NotImplementedError("todo: write parse()")


def to_sexpr(node: Node) -> str:
    """The tree as nested parentheses: (+ 1 (* 2 x)), (neg x), (call max a b)."""
    raise NotImplementedError("todo: write to_sexpr()")


BUILTINS: dict[str, Callable[..., int | float]] = {
    "abs": abs, "min": min, "max": max, "sqrt": math.sqrt,
}


def evaluate(node: Node, env: dict[str, int | float] | None = None) -> int | float:
    """The value of a tree, with variables from env and the BUILTINS functions."""
    raise NotImplementedError("todo: write evaluate()")

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
    tokens: list[Token] = []
    i = 0
    while i < len(source):
        c = source[i]
        if c.isspace():
            i += 1
        elif c.isascii() and c.isdigit():
            start = i
            while i < len(source) and source[i].isascii() and source[i].isdigit():
                i += 1
            if i + 1 < len(source) and source[i] == "." and source[i + 1].isascii() and source[i + 1].isdigit():
                i += 1
                while i < len(source) and source[i].isascii() and source[i].isdigit():
                    i += 1
            tokens.append(Token("number", source[start:i], start))
        elif c.isascii() and (c.isalpha() or c == "_"):
            start = i
            while i < len(source) and source[i].isascii() and (source[i].isalnum() or source[i] == "_"):
                i += 1
            tokens.append(Token("ident", source[start:i], start))
        elif c in OPERATORS:
            tokens.append(Token("op", c, i))
            i += 1
        else:
            raise ParseError(f"unexpected character {c!r}", i)
    tokens.append(Token("eof", "", len(source)))
    return tokens


class _Parser:
    def __init__(self, source: str):
        self.tokens = tokenize(source)
        self.i = 0

    def peek(self) -> Token:
        return self.tokens[self.i]

    def advance(self) -> Token:
        token = self.tokens[self.i]
        self.i += 1
        return token

    def at(self, *texts: str) -> bool:
        token = self.peek()
        return token.kind == "op" and token.text in texts

    def expect(self, text: str) -> Token:
        if not self.at(text):
            token = self.peek()
            found = "end of input" if token.kind == "eof" else repr(token.text)
            raise ParseError(f"expected {text!r} but found {found}", token.pos)
        return self.advance()

    def expr(self) -> Node:
        node = self.term()
        while self.at("+", "-"):
            op = self.advance().text
            node = Binary(op, node, self.term())
        return node

    def term(self) -> Node:
        node = self.unary()
        while self.at("*", "/", "%"):
            op = self.advance().text
            node = Binary(op, node, self.unary())
        return node

    def unary(self) -> Node:
        if self.at("-"):
            self.advance()
            return Unary("-", self.unary())
        return self.power()

    def power(self) -> Node:
        base = self.atom()
        if self.at("^"):
            self.advance()
            return Binary("^", base, self.unary())
        return base

    def atom(self) -> Node:
        token = self.advance()
        if token.kind == "number":
            return Num(float(token.text) if "." in token.text else int(token.text))
        if token.kind == "ident":
            if not self.at("("):
                return Var(token.text)
            self.advance()
            args: list[Node] = []
            if not self.at(")"):
                args.append(self.expr())
                while self.at(","):
                    self.advance()
                    args.append(self.expr())
            self.expect(")")
            return Call(token.text, tuple(args))
        if token.kind == "op" and token.text == "(":
            node = self.expr()
            self.expect(")")
            return node
        found = "end of input" if token.kind == "eof" else repr(token.text)
        raise ParseError(f"expected a number, a name or '(' but found {found}", token.pos)


def parse(source: str) -> Node:
    parser = _Parser(source)
    node = parser.expr()
    token = parser.peek()
    if token.kind != "eof":
        raise ParseError(f"unexpected {token.text!r} after the expression", token.pos)
    return node


def to_sexpr(node: Node) -> str:
    match node:
        case Num(value):
            return repr(value)
        case Var(name):
            return name
        case Unary(_, operand):
            return f"(neg {to_sexpr(operand)})"
        case Binary(op, left, right):
            return f"({op} {to_sexpr(left)} {to_sexpr(right)})"
        case Call(name, args):
            return "(call " + " ".join([name, *(to_sexpr(a) for a in args)]) + ")"
    raise TypeError(f"not a node: {node!r}")


BUILTINS: dict[str, Callable[..., int | float]] = {
    "abs": abs, "min": min, "max": max, "sqrt": math.sqrt,
}


def evaluate(node: Node, env: dict[str, int | float] | None = None) -> int | float:
    env = env or {}
    match node:
        case Num(value):
            return value
        case Var(name):
            if name not in env:
                raise EvalError(f"unknown variable {name!r}")
            return env[name]
        case Unary(_, operand):
            return -evaluate(operand, env)
        case Binary(op, left, right):
            a, b = evaluate(left, env), evaluate(right, env)
            if (op in "/%" and b == 0) or (op == "^" and a == 0 and b < 0):
                raise EvalError("division by zero")
            match op:
                case "+":
                    return a + b
                case "-":
                    return a - b
                case "*":
                    return a * b
                case "/":
                    return a / b
                case "%":
                    return a % b
                case "^":
                    return a**b
        case Call(name, args):
            if name not in BUILTINS:
                raise EvalError(f"unknown function {name!r}")
            values = [evaluate(a, env) for a in args]
            try:
                return BUILTINS[name](*values)
            except (TypeError, ValueError) as error:
                raise EvalError(f"bad call to {name}: {error}") from error
    raise TypeError(f"not a node: {node!r}")

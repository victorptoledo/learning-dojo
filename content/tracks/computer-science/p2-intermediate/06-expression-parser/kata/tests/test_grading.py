import random

import pytest

from expr import (
    Binary, Call, EvalError, Num, ParseError, Token, Unary, Var, evaluate, parse, to_sexpr, tokenize,
)


def tree(source):
    return to_sexpr(parse(source))


# ---- the lexer ----------------------------------------------------------------------------

def test_tokens_have_kinds_texts_and_positions():
    assert tokenize("  max(a_1, 3.25)^2 ") == [
        Token("ident", "max", 2), Token("op", "(", 5), Token("ident", "a_1", 6),
        Token("op", ",", 9), Token("number", "3.25", 11), Token("op", ")", 15),
        Token("op", "^", 16), Token("number", "2", 17), Token("eof", "", 19),
    ]


def test_empty_input_is_just_eof():
    assert tokenize("") == [Token("eof", "", 0)]
    assert tokenize("   ") == [Token("eof", "", 3)]


def test_numbers_need_digits_on_both_sides_of_the_point():
    assert [t.text for t in tokenize("10 2.5 7")[:-1]] == ["10", "2.5", "7"]
    with pytest.raises(ParseError) as error:
        tokenize("3.")
    assert error.value.pos == 1
    with pytest.raises(ParseError) as error:
        tokenize("1.2.3")
    assert error.value.pos == 3


def test_unknown_characters_are_rejected_with_their_position():
    for source, pos in [("1 & 2", 2), ("a = 1", 2), ("x#", 1), ("é", 0)]:
        with pytest.raises(ParseError) as error:
            tokenize(source)
        assert error.value.pos == pos, source


# ---- the parser ---------------------------------------------------------------------------

@pytest.mark.parametrize(
    "source, expected",
    [
        ("42", "42"),
        ("2.5", "2.5"),
        ("x", "x"),
        ("1 + 2 * 3", "(+ 1 (* 2 3))"),
        ("(1 + 2) * 3", "(* (+ 1 2) 3)"),
        ("a - b - c", "(- (- a b) c)"),
        ("a / b * c % d", "(% (* (/ a b) c) d)"),
        ("2 ^ 3 ^ 2", "(^ 2 (^ 3 2))"),
        ("a - b - c * 2 ^ 3 ^ 2", "(- (- a b) (* c (^ 2 (^ 3 2))))"),
        ("-x", "(neg x)"),
        ("--x", "(neg (neg x))"),
        ("-2 ^ 2", "(neg (^ 2 2))"),
        ("2 ^ -1", "(^ 2 (neg 1))"),
        ("2 * -3", "(* 2 (neg 3))"),
        ("1 - -1", "(- 1 (neg 1))"),
        ("f()", "(call f)"),
        ("max(a, b + 1, 3)", "(call max a (+ b 1) 3)"),
        ("max(1, min(2, 3)) ^ 2", "(^ (call max 1 (call min 2 3)) 2)"),
        ("((((x))))", "x"),
    ],
)
def test_trees(source, expected):
    assert tree(source) == expected


def test_parse_builds_the_node_classes():
    assert parse("-f(x) + 2.0") == Binary("+", Unary("-", Call("f", (Var("x"),))), Num(2.0))
    assert parse("7") == Num(7) and isinstance(parse("7").value, int)


def test_deep_nesting():
    source = "(" * 100 + "1" + ")" * 100
    assert tree(source) == "1"
    assert evaluate(parse(" + ".join(["1"] * 300))) == 300


@pytest.mark.parametrize(
    "source, pos",
    [
        ("", 0),
        ("1 +", 3),
        ("(1 + 2", 6),
        ("1 + 2)", 5),
        ("1 2", 2),
        ("* 3", 0),
        ("f(1,)", 4),
        ("f(1 2)", 4),
        ("x y", 2),
        ("()", 1),
        ("2 ^", 3),
    ],
)
def test_syntax_errors_point_at_the_offending_token(source, pos):
    with pytest.raises(ParseError) as error:
        parse(source)
    assert error.value.pos == pos
    assert f"position {pos}" in str(error.value)


# ---- the evaluator ------------------------------------------------------------------------

def test_evaluate_arithmetic():
    assert evaluate(parse("a - b - c * 2 ^ 3 ^ 2"), {"a": 10, "b": 3, "c": 1}) == -505
    assert evaluate(parse("7 / 2")) == 3.5
    assert evaluate(parse("7 % 3")) == 1
    assert evaluate(parse("-2 ^ 2")) == -4
    assert evaluate(parse("2 ^ -1")) == 0.5
    assert evaluate(parse("0.1 + 0.2")) == 0.1 + 0.2


def test_evaluate_functions():
    assert evaluate(parse("max(1, x, 3)"), {"x": 9}) == 9
    assert evaluate(parse("min(4, 2) + abs(-5)")) == 7
    assert evaluate(parse("sqrt(16)")) == 4.0


@pytest.mark.parametrize(
    "source", ["y + 1", "nope(1)", "1 / 0", "5 % (2 - 2)", "0 ^ -1", "sqrt(-1)", "min()", "abs(1, 2)"],
)
def test_evaluation_errors(source):
    with pytest.raises(EvalError):
        evaluate(parse(source), {"x": 1})


def test_variables_do_not_need_an_environment_when_absent():
    assert evaluate(parse("1 + 2")) == 3


def _random_expression(rng, depth):
    if depth == 0 or rng.random() < 0.25:
        return rng.choice([str(rng.randint(0, 20)), "x", "y"])
    kind = rng.random()
    if kind < 0.15:
        return "-" + _random_expression(rng, depth - 1)
    if kind < 0.3:
        return "(" + _random_expression(rng, depth - 1) + ")"
    if kind < 0.38:
        return "(" + _random_expression(rng, depth - 1) + ") ^ " + str(rng.randint(0, 3))
    op = rng.choice(["+", "-", "*", "+", "-"])
    return _random_expression(rng, depth - 1) + f" {op} " + _random_expression(rng, depth - 1)


def test_random_expressions_agree_with_python():
    rng = random.Random(2026)
    env = {"x": 3, "y": -7}
    for _ in range(1500):
        source = _random_expression(rng, 5)
        expected = eval(source.replace("^", "**"), {}, dict(env))  # noqa: S307
        assert evaluate(parse(source), env) == expected, source

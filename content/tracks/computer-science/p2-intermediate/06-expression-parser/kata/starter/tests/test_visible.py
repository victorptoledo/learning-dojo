from expr import Token, evaluate, parse, to_sexpr, tokenize


def test_tokens_of_a_small_expression():
    assert tokenize("x*2") == [
        Token("ident", "x", 0), Token("op", "*", 1), Token("number", "2", 2), Token("eof", "", 3),
    ]


def test_precedence_shows_in_the_tree():
    assert to_sexpr(parse("1 + 2 * x")) == "(+ 1 (* 2 x))"


def test_evaluate_with_a_variable():
    assert evaluate(parse("1 + 2 * x"), {"x": 10}) == 21

import pytest

from greet import greet, main


def test_greets_a_full_name():
    assert greet("Grace Hopper") == "Welcome to the dojo, Grace Hopper!"


def test_strips_spaces_around_the_name():
    assert greet("  Ada  ") == "Welcome to the dojo, Ada!"


@pytest.mark.parametrize("blank", ["", "   ", "\t\n"])
def test_greets_a_stranger_when_the_name_is_blank(blank):
    assert greet(blank) == "Welcome to the dojo, stranger!"


def test_name_defaults_to_a_stranger():
    assert greet() == "Welcome to the dojo, stranger!"


def test_shouts_in_upper_case():
    assert greet("Ada", shout=True) == "WELCOME TO THE DOJO, ADA!"


def test_shout_is_keyword_only():
    with pytest.raises(TypeError):
        greet("Ada", True)


def test_main_joins_the_words_and_returns_zero(capsys):
    assert main(["Ada", "Lovelace"]) == 0
    assert capsys.readouterr().out == "Welcome to the dojo, Ada Lovelace!\n"


def test_main_shouts_with_the_flag_anywhere(capsys):
    assert main(["Ada", "--shout"]) == 0
    assert main([]) == 0
    assert capsys.readouterr().out == "WELCOME TO THE DOJO, ADA!\nWelcome to the dojo, stranger!\n"

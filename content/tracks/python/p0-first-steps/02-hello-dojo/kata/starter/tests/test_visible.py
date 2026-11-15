from greet import greet


def test_greets_by_name():
    assert greet("Ada") == "Welcome to the dojo, Ada!"

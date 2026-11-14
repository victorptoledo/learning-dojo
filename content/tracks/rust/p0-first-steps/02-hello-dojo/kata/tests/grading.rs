use kata::greet;

#[test]
fn greets_another_name() {
    assert_eq!(greet("Ada"), "Welcome to the dojo, Ada!");
}

#[test]
fn ignores_surrounding_spaces() {
    assert_eq!(greet("  Ferris \t"), "Welcome to the dojo, Ferris!");
}

#[test]
fn greets_a_stranger_when_the_name_is_empty() {
    assert_eq!(greet(""), "Welcome to the dojo, stranger!");
}

#[test]
fn greets_a_stranger_when_the_name_is_blank() {
    assert_eq!(greet("   "), "Welcome to the dojo, stranger!");
}

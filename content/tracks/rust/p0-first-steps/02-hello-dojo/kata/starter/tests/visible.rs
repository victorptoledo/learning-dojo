use kata::greet;

#[test]
fn greets_by_name() {
    assert_eq!(greet("Ferris"), "Welcome to the dojo, Ferris!");
}

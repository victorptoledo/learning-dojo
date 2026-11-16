use kata::{Regex, RegexError};

#[test]
fn star_and_alternation() {
    let re = Regex::new("(ab|c)*d").unwrap();
    assert!(re.is_match("ababcd"));
    assert!(re.is_match("d"));
    assert!(!re.is_match("abd!"));
}

#[test]
fn a_lonely_star_is_an_error() {
    assert_eq!(Regex::new("*a").err(), Some(RegexError::NothingToRepeat(0)));
}

use kata::{letter_counts, unique_words};

#[test]
fn counts_the_letters_of_banana() {
    assert_eq!(letter_counts("banana"), [('a', 3), ('n', 2), ('b', 1)]);
}

#[test]
fn distinct_words_are_sorted() {
    assert_eq!(unique_words("b a B"), ["a", "b"]);
}

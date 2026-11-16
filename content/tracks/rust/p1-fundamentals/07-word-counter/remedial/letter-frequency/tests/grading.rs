use kata::{letter_counts, unique_words};

#[test]
fn ties_are_in_alphabetical_order() {
    assert_eq!(letter_counts("Hello"), [('l', 2), ('e', 1), ('h', 1), ('o', 1)]);
}

#[test]
fn case_is_ignored() {
    assert_eq!(letter_counts("AaBb"), [('a', 2), ('b', 2)]);
}

#[test]
fn digits_spaces_and_punctuation_are_not_letters() {
    assert!(letter_counts("123 !! ?").is_empty());
    assert_eq!(letter_counts("a1 a2, b!"), [('a', 2), ('b', 1)]);
}

#[test]
fn accented_letters_count() {
    assert_eq!(letter_counts("Éé e"), [('é', 2), ('e', 1)]);
}

#[test]
fn words_lose_punctuation_and_case() {
    assert_eq!(unique_words("The cat, the HAT. cat!"), ["cat", "hat", "the"]);
}

#[test]
fn punctuation_alone_is_not_a_word() {
    assert!(unique_words("--- ... !").is_empty());
    assert!(unique_words("").is_empty());
}

#[test]
fn inner_punctuation_stays() {
    assert_eq!(unique_words("don't stop, don't"), ["don't", "stop"]);
}

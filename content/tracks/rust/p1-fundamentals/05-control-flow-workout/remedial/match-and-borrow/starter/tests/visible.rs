use kata::{first_word_at_least, grade, sum_until_negative};

#[test]
fn ninety_five_is_an_a() {
    assert_eq!(grade(95), Some('A'));
}

#[test]
fn stops_at_the_first_negative() {
    assert_eq!(sum_until_negative(&[3, 4, -2, 10]), 7);
}

#[test]
fn finds_a_long_enough_word() {
    assert_eq!(first_word_at_least("the borrow checker", 5), Some("borrow"));
}

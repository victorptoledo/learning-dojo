use kata::{first_word_at_least, grade, sum_until_negative};

#[test]
fn grade_boundaries() {
    assert_eq!(grade(100), Some('A'));
    assert_eq!(grade(90), Some('A'));
    assert_eq!(grade(89), Some('B'));
    assert_eq!(grade(80), Some('B'));
    assert_eq!(grade(79), Some('C'));
    assert_eq!(grade(70), Some('C'));
    assert_eq!(grade(69), Some('D'));
    assert_eq!(grade(60), Some('D'));
    assert_eq!(grade(59), Some('F'));
    assert_eq!(grade(0), Some('F'));
}

#[test]
fn above_a_hundred_has_no_grade() {
    assert_eq!(grade(101), None);
    assert_eq!(grade(u32::MAX), None);
}

#[test]
fn sum_of_nothing_is_zero() {
    assert_eq!(sum_until_negative(&[]), 0);
}

#[test]
fn a_leading_negative_gives_zero() {
    assert_eq!(sum_until_negative(&[-1, 4, 5]), 0);
}

#[test]
fn without_negatives_everything_is_added() {
    assert_eq!(sum_until_negative(&[4, 5, 0, 6]), 15);
}

#[test]
fn zero_is_not_negative() {
    assert_eq!(sum_until_negative(&[0, 0, 2, -3, 9]), 2);
}

#[test]
fn the_first_match_wins() {
    assert_eq!(first_word_at_least("a bb ccc dddd", 3), Some("ccc"));
}

#[test]
fn characters_are_counted_not_bytes() {
    assert_eq!(first_word_at_least("né café", 4), Some("café"));
    assert_eq!(first_word_at_least("né", 3), None);
}

#[test]
fn no_word_is_long_enough() {
    assert_eq!(first_word_at_least("short words", 10), None);
    assert_eq!(first_word_at_least("", 0), None);
}

#[test]
fn the_word_is_borrowed_from_the_text() {
    let text = String::from("a bb ccc");
    let word = first_word_at_least(&text, 3).unwrap();
    assert_eq!(word.as_ptr(), text[5..].as_ptr());
}

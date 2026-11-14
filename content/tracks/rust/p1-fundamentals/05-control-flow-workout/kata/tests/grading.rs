use kata::{classify, collatz_steps, fizzbuzz, longest_word};

#[test]
fn fizzbuzz_marks_multiples_of_fifteen() {
    let values = fizzbuzz(15);
    assert_eq!(values.len(), 15);
    assert_eq!(values[14], "FizzBuzz");
    assert_eq!(values[8], "Fizz");
    assert_eq!(values[9], "Buzz");
    assert_eq!(values[6], "7");
}

#[test]
fn fizzbuzz_of_zero_is_empty() {
    assert!(fizzbuzz(0).is_empty());
}

#[test]
fn collatz_of_one_takes_no_steps() {
    assert_eq!(collatz_steps(1), 0);
}

#[test]
fn collatz_of_twenty_seven() {
    assert_eq!(collatz_steps(27), 111);
}

#[test]
fn classify_covers_every_range() {
    assert_eq!(classify(-3), "negative");
    assert_eq!(classify(i32::MIN), "negative");
    assert_eq!(classify(1), "small");
    assert_eq!(classify(9), "small");
    assert_eq!(classify(10), "large");
    assert_eq!(classify(i32::MAX), "large");
}

#[test]
fn longest_word_keeps_the_first_on_a_tie() {
    assert_eq!(longest_word("ab cd ef"), Some("ab"));
}

#[test]
fn longest_word_ignores_extra_whitespace() {
    assert_eq!(longest_word("  fearless\tconcurrency  \n"), Some("concurrency"));
}

#[test]
fn longest_word_of_blank_text_is_none() {
    assert_eq!(longest_word(""), None);
    assert_eq!(longest_word(" \t\n"), None);
}

#[test]
fn longest_word_borrows_from_the_input() {
    let text = String::from("ownership rules");
    let word = longest_word(&text).unwrap();
    assert!(text.as_ptr() <= word.as_ptr());
}

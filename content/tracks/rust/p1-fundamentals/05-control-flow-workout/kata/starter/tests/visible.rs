use kata::{classify, collatz_steps, fizzbuzz, longest_word};

#[test]
fn fizzbuzz_up_to_five() {
    assert_eq!(fizzbuzz(5), ["1", "2", "Fizz", "4", "Buzz"]);
}

#[test]
fn collatz_of_six() {
    assert_eq!(collatz_steps(6), 8);
}

#[test]
fn classify_zero() {
    assert_eq!(classify(0), "zero");
}

#[test]
fn longest_word_of_a_sentence() {
    assert_eq!(longest_word("the borrow checker"), Some("checker"));
}

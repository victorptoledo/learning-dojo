/// FizzBuzz values from 1 to `n`.
pub fn fizzbuzz(n: u32) -> Vec<String> {
    (1..=n).map(fizzbuzz_value).collect()
}

fn fizzbuzz_value(i: u32) -> String {
    match (i % 3, i % 5) {
        (0, 0) => "FizzBuzz".to_string(),
        (0, _) => "Fizz".to_string(),
        (_, 0) => "Buzz".to_string(),
        _ => i.to_string(),
    }
}

/// Steps of the Collatz sequence needed to reach 1.
pub fn collatz_steps(n: u64) -> u32 {
    let mut value = n;
    let mut steps = 0;
    while value > 1 {
        value = if value % 2 == 0 { value / 2 } else { 3 * value + 1 };
        steps += 1;
    }
    steps
}

/// Size class of `n`: negative, zero, small (1..=9) or large.
pub fn classify(n: i32) -> &'static str {
    match n {
        i32::MIN..=-1 => "negative",
        0 => "zero",
        1..=9 => "small",
        _ => "large",
    }
}

/// Longest word in `text`, borrowed from it. The first one wins on a tie.
pub fn longest_word(text: &str) -> Option<&str> {
    text.split_whitespace()
        .fold(None, |longest: Option<&str>, word| match longest {
            Some(current) if current.len() >= word.len() => Some(current),
            _ => Some(word),
        })
}

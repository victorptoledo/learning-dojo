/// Letter grade of a score out of 100, or `None` above 100.
pub fn grade(score: u32) -> Option<char> {
    match score {
        90..=100 => Some('A'),
        80..=89 => Some('B'),
        70..=79 => Some('C'),
        60..=69 => Some('D'),
        0..=59 => Some('F'),
        _ => None,
    }
}

/// Sum of the values before the first negative one.
pub fn sum_until_negative(values: &[i32]) -> i32 {
    let mut total = 0;
    for &value in values {
        if value < 0 {
            break;
        }
        total += value;
    }
    total
}

/// First word of `text` with at least `min_len` characters, borrowed from `text`.
pub fn first_word_at_least(text: &str, min_len: usize) -> Option<&str> {
    text.split_whitespace()
        .find(|word| word.chars().count() >= min_len)
}

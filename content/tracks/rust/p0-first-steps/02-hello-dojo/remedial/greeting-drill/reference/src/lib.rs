/// `name` in capitals followed by `!`.
pub fn shout(name: &str) -> String {
    format!("{}!", name.to_uppercase())
}

/// The first letter of each word, in capitals.
pub fn initials(full_name: &str) -> String {
    full_name
        .split_whitespace()
        .filter_map(|word| word.chars().next())
        .flat_map(char::to_uppercase)
        .collect()
}

/// `word` repeated `times` times, separated by single spaces.
pub fn echo(word: &str, times: usize) -> String {
    vec![word; times].join(" ")
}

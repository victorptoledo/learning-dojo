//! Stage 1: numbers and the four operators, with precedence and left-to-right order.

/// Splits the input into numbers and operator characters, ignoring spaces.
fn tokenize(input: &str) -> Vec<String> {
    let mut tokens = Vec::new();
    let mut number = String::new();
    for c in input.chars() {
        if c.is_ascii_digit() || c == '.' {
            number.push(c);
            continue;
        }
        if !number.is_empty() {
            tokens.push(std::mem::take(&mut number));
        }
        if !c.is_whitespace() {
            tokens.push(c.to_string());
        }
    }
    if !number.is_empty() {
        tokens.push(number);
    }
    tokens
}

/// `term (('+' | '-') term)*` where `term` is `number (('*' | '/') number)*`.
fn evaluate(tokens: &[String]) -> f64 {
    let mut position = 0;
    let mut total = term(tokens, &mut position);
    while position < tokens.len() {
        let operator = tokens[position].as_str();
        position += 1;
        let next = term(tokens, &mut position);
        match operator {
            "+" => total += next,
            _ => total -= next,
        }
    }
    total
}

fn term(tokens: &[String], position: &mut usize) -> f64 {
    let mut value: f64 = tokens[*position].parse().unwrap_or(0.0);
    *position += 1;
    while *position < tokens.len() && matches!(tokens[*position].as_str(), "*" | "/") {
        let operator = tokens[*position].clone();
        let next: f64 = tokens[*position + 1].parse().unwrap_or(0.0);
        *position += 2;
        if operator == "*" {
            value *= next;
        } else {
            value /= next;
        }
    }
    value
}

fn main() {
    let expression = std::env::args().nth(1).unwrap_or_default();
    println!("{}", evaluate(&tokenize(&expression)));
}

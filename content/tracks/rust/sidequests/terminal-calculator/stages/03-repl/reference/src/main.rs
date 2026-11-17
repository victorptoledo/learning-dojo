use std::collections::HashMap;
use std::fmt;
use std::io::{self, BufRead};
use std::process::ExitCode;

#[derive(Debug, Clone, PartialEq)]
enum Token {
    Number(f64),
    Name(String),
    Plus,
    Minus,
    Star,
    Slash,
    Open,
    Close,
}

/// A token and the 1-based position of its first character.
#[derive(Debug, Clone)]
struct Spanned {
    token: Token,
    text: String,
    position: usize,
}

#[derive(Debug, PartialEq)]
enum CalcError {
    Empty,
    DivisionByZero,
    UnexpectedChar(char, usize),
    Unexpected(String, usize),
    ExpectedNumber,
    ExpectedClose,
    UnknownVariable(String),
}

impl fmt::Display for CalcError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Empty => write!(f, "empty expression"),
            Self::DivisionByZero => write!(f, "division by zero"),
            Self::UnexpectedChar(c, at) => write!(f, "unexpected character '{c}' at position {at}"),
            Self::Unexpected(text, at) => write!(f, "unexpected '{text}' at position {at}"),
            Self::ExpectedNumber => write!(f, "expected a number"),
            Self::ExpectedClose => write!(f, "expected ')'"),
            Self::UnknownVariable(name) => write!(f, "unknown variable '{name}'"),
        }
    }
}

/// With `names` off (an argument, not the REPL) a letter is an unexpected character.
fn tokenize(input: &str, names: bool) -> Result<Vec<Spanned>, CalcError> {
    let chars: Vec<char> = input.chars().collect();
    let mut tokens = Vec::new();
    let mut i = 0;
    while i < chars.len() {
        let c = chars[i];
        let position = i + 1;
        if c.is_whitespace() {
            i += 1;
            continue;
        }
        if c.is_ascii_digit() || c == '.' {
            let start = i;
            while i < chars.len() && (chars[i].is_ascii_digit() || chars[i] == '.') {
                i += 1;
            }
            let text: String = chars[start..i].iter().collect();
            let value = text
                .parse()
                .map_err(|_| CalcError::Unexpected(text.clone(), position))?;
            tokens.push(Spanned {
                token: Token::Number(value),
                text,
                position,
            });
            continue;
        }
        if names && c.is_alphabetic() {
            let start = i;
            while i < chars.len() && (chars[i].is_alphanumeric() || chars[i] == '_') {
                i += 1;
            }
            let text: String = chars[start..i].iter().collect();
            tokens.push(Spanned {
                token: Token::Name(text.clone()),
                text,
                position,
            });
            continue;
        }
        let token = match c {
            '+' => Token::Plus,
            '-' => Token::Minus,
            '*' => Token::Star,
            '/' => Token::Slash,
            '(' => Token::Open,
            ')' => Token::Close,
            other => return Err(CalcError::UnexpectedChar(other, position)),
        };
        tokens.push(Spanned {
            token,
            text: c.to_string(),
            position,
        });
        i += 1;
    }
    Ok(tokens)
}

/// Recursive descent that evaluates as it parses.
struct Parser<'a> {
    tokens: Vec<Spanned>,
    next: usize,
    variables: &'a HashMap<String, f64>,
}

impl Parser<'_> {
    fn peek(&self) -> Option<&Token> {
        self.tokens.get(self.next).map(|spanned| &spanned.token)
    }

    fn expression(&mut self) -> Result<f64, CalcError> {
        let mut value = self.term()?;
        loop {
            match self.peek() {
                Some(Token::Plus) => {
                    self.next += 1;
                    value += self.term()?;
                }
                Some(Token::Minus) => {
                    self.next += 1;
                    value -= self.term()?;
                }
                _ => return Ok(value),
            }
        }
    }

    fn term(&mut self) -> Result<f64, CalcError> {
        let mut value = self.factor()?;
        loop {
            match self.peek() {
                Some(Token::Star) => {
                    self.next += 1;
                    value *= self.factor()?;
                }
                Some(Token::Slash) => {
                    self.next += 1;
                    let divisor = self.factor()?;
                    if divisor == 0.0 {
                        return Err(CalcError::DivisionByZero);
                    }
                    value /= divisor;
                }
                _ => return Ok(value),
            }
        }
    }

    fn factor(&mut self) -> Result<f64, CalcError> {
        let Some(token) = self.peek().cloned() else {
            return Err(CalcError::ExpectedNumber);
        };
        self.next += 1;
        match token {
            Token::Number(value) => Ok(value),
            Token::Name(name) => self
                .variables
                .get(&name)
                .copied()
                .ok_or(CalcError::UnknownVariable(name)),
            Token::Minus => Ok(-self.factor()?),
            Token::Open => {
                let value = self.expression()?;
                if self.peek() == Some(&Token::Close) {
                    self.next += 1;
                    Ok(value)
                } else {
                    Err(CalcError::ExpectedClose)
                }
            }
            Token::Plus | Token::Star | Token::Slash | Token::Close => {
                Err(CalcError::ExpectedNumber)
            }
        }
    }
}

fn evaluate(input: &str, variables: Option<&HashMap<String, f64>>) -> Result<f64, CalcError> {
    let tokens = tokenize(input, variables.is_some())?;
    let empty = HashMap::new();
    let variables = variables.unwrap_or(&empty);
    if tokens.is_empty() {
        return Err(CalcError::Empty);
    }
    let mut parser = Parser {
        tokens,
        next: 0,
        variables,
    };
    let value = parser.expression()?;
    match parser.tokens.get(parser.next) {
        Some(extra) => Err(CalcError::Unexpected(extra.text.clone(), extra.position)),
        None => Ok(value),
    }
}

#[derive(Default)]
struct Session {
    variables: HashMap<String, f64>,
}

impl Session {
    /// One REPL line: an expression or `let name = expression`. Returns what to print.
    fn line(&mut self, line: &str) -> Result<String, CalcError> {
        let (name, expression) = match line.trim().strip_prefix("let ") {
            Some(rest) => match rest.split_once('=') {
                Some((name, expression)) if valid_name(name.trim()) => {
                    (Some(name.trim().to_string()), expression)
                }
                _ => return Err(CalcError::Unexpected(rest.trim().to_string(), 5)),
            },
            None => (None, line),
        };
        let value = evaluate(expression, Some(&self.variables))?;
        self.variables.insert("ans".to_string(), value);
        Ok(match name {
            Some(name) => {
                self.variables.insert(name.clone(), value);
                format!("{name} = {value}")
            }
            None => value.to_string(),
        })
    }
}

fn valid_name(name: &str) -> bool {
    let mut chars = name.chars();
    chars.next().is_some_and(char::is_alphabetic) && chars.all(|c| c.is_alphanumeric() || c == '_')
}

fn main() -> ExitCode {
    if let Some(expression) = std::env::args().nth(1) {
        return match evaluate(&expression, None) {
            Ok(value) => {
                println!("{value}");
                ExitCode::SUCCESS
            }
            Err(error) => {
                eprintln!("error: {error}");
                ExitCode::FAILURE
            }
        };
    }
    let mut session = Session::default();
    let mut failed = false;
    for line in io::stdin().lock().lines() {
        let Ok(line) = line else { break };
        if line.trim().is_empty() {
            continue;
        }
        match session.line(&line) {
            Ok(output) => println!("{output}"),
            Err(error) => {
                eprintln!("error: {error}");
                failed = true;
            }
        }
    }
    if failed {
        ExitCode::FAILURE
    } else {
        ExitCode::SUCCESS
    }
}

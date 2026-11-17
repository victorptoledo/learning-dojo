use std::fmt;
use std::process::ExitCode;

#[derive(Debug, Clone, PartialEq)]
enum Token {
    Number(f64),
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
        }
    }
}

fn tokenize(input: &str) -> Result<Vec<Spanned>, CalcError> {
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
struct Parser {
    tokens: Vec<Spanned>,
    next: usize,
}

impl Parser {
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

fn evaluate(input: &str) -> Result<f64, CalcError> {
    let tokens = tokenize(input)?;
    if tokens.is_empty() {
        return Err(CalcError::Empty);
    }
    let mut parser = Parser { tokens, next: 0 };
    let value = parser.expression()?;
    match parser.tokens.get(parser.next) {
        Some(extra) => Err(CalcError::Unexpected(extra.text.clone(), extra.position)),
        None => Ok(value),
    }
}

fn main() -> ExitCode {
    let expression = std::env::args().nth(1).unwrap_or_default();
    match evaluate(&expression) {
        Ok(value) => {
            println!("{value}");
            ExitCode::SUCCESS
        }
        Err(error) => {
            eprintln!("error: {error}");
            ExitCode::FAILURE
        }
    }
}

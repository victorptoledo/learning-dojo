//! A regular expression engine: a parser, Thompson's construction and an NFA simulation
//! that runs in O(pattern × text) time, whatever the pattern.

/// What is wrong with a pattern; positions count characters from 0.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RegexError {
    /// A `)` without its `(`, or a `(` that is never closed (at the `(`).
    UnbalancedParen(usize),
    /// A `*`, `+` or `?` with nothing to repeat.
    NothingToRepeat(usize),
    /// A `\` at the end of the pattern.
    TrailingBackslash(usize),
    /// A `[` that is never closed.
    UnclosedClass(usize),
    /// `[]` or `[^]`.
    EmptyClass(usize),
    /// A range such as `z-a` whose start is after its end (at the start character).
    BadRange(usize),
}

#[derive(Debug, Clone)]
enum Node {
    Empty,
    Set(CharSet),
    Concat(Vec<Node>),
    Alt(Box<Node>, Box<Node>),
    Star(Box<Node>),
    Plus(Box<Node>),
    Optional(Box<Node>),
}

#[derive(Debug, Clone)]
struct CharSet {
    ranges: Vec<(char, char)>,
    negated: bool,
}

impl CharSet {
    fn single(c: char) -> CharSet {
        CharSet { ranges: vec![(c, c)], negated: false }
    }

    fn any() -> CharSet {
        CharSet { ranges: Vec::new(), negated: true }
    }

    fn contains(&self, c: char) -> bool {
        self.ranges.iter().any(|&(lo, hi)| lo <= c && c <= hi) != self.negated
    }
}

struct Parser {
    chars: Vec<char>,
    pos: usize,
}

impl Parser {
    fn peek(&self) -> Option<char> {
        self.chars.get(self.pos).copied()
    }

    /// alternation := concat ("|" concat)*
    fn alternation(&mut self) -> Result<Node, RegexError> {
        let mut node = self.concat()?;
        while self.peek() == Some('|') {
            self.pos += 1;
            let right = self.concat()?;
            node = Node::Alt(Box::new(node), Box::new(right));
        }
        Ok(node)
    }

    /// concat := repeat*
    fn concat(&mut self) -> Result<Node, RegexError> {
        let mut items = Vec::new();
        while let Some(c) = self.peek() {
            if c == '|' || c == ')' {
                break;
            }
            items.push(self.repeat()?);
        }
        Ok(match items.len() {
            0 => Node::Empty,
            1 => items.pop().expect("one item"),
            _ => Node::Concat(items),
        })
    }

    /// repeat := atom ("*" | "+" | "?")?
    fn repeat(&mut self) -> Result<Node, RegexError> {
        let atom = self.atom()?;
        let node = match self.peek() {
            Some('*') => Node::Star(Box::new(atom)),
            Some('+') => Node::Plus(Box::new(atom)),
            Some('?') => Node::Optional(Box::new(atom)),
            _ => return Ok(atom),
        };
        self.pos += 1;
        if let Some('*' | '+' | '?') = self.peek() {
            return Err(RegexError::NothingToRepeat(self.pos));
        }
        Ok(node)
    }

    fn atom(&mut self) -> Result<Node, RegexError> {
        let start = self.pos;
        let c = self.peek().expect("concat checked for input");
        self.pos += 1;
        match c {
            '*' | '+' | '?' => Err(RegexError::NothingToRepeat(start)),
            '.' => Ok(Node::Set(CharSet::any())),
            '(' => {
                let inner = self.alternation()?;
                if self.peek() != Some(')') {
                    return Err(RegexError::UnbalancedParen(start));
                }
                self.pos += 1;
                Ok(inner)
            }
            '[' => self.class(start),
            '\\' => match self.peek() {
                Some(escaped) => {
                    self.pos += 1;
                    Ok(Node::Set(CharSet::single(escaped)))
                }
                None => Err(RegexError::TrailingBackslash(start)),
            },
            _ => Ok(Node::Set(CharSet::single(c))),
        }
    }

    fn class(&mut self, start: usize) -> Result<Node, RegexError> {
        let mut set = CharSet { ranges: Vec::new(), negated: false };
        if self.peek() == Some('^') {
            set.negated = true;
            self.pos += 1;
        }
        loop {
            let Some(c) = self.peek() else {
                return Err(RegexError::UnclosedClass(start));
            };
            if c == ']' {
                self.pos += 1;
                break;
            }
            let at = self.pos;
            let lo = self.class_char(start)?;
            if self.peek() == Some('-') && self.chars.get(self.pos + 1).is_some_and(|&n| n != ']') {
                self.pos += 1;
                let hi = self.class_char(start)?;
                if lo > hi {
                    return Err(RegexError::BadRange(at));
                }
                set.ranges.push((lo, hi));
            } else {
                set.ranges.push((lo, lo));
            }
        }
        if set.ranges.is_empty() {
            return Err(RegexError::EmptyClass(start));
        }
        Ok(Node::Set(set))
    }

    fn class_char(&mut self, start: usize) -> Result<char, RegexError> {
        let c = self.peek().ok_or(RegexError::UnclosedClass(start))?;
        self.pos += 1;
        if c != '\\' {
            return Ok(c);
        }
        let escaped = self.peek().ok_or(RegexError::UnclosedClass(start))?;
        self.pos += 1;
        Ok(escaped)
    }
}

#[derive(Debug, Clone)]
enum State {
    /// Consume one character in the set, then go to `next`.
    Char(CharSet, usize),
    /// Go to both, without consuming anything.
    Split(usize, usize),
    /// Go to `next` without consuming anything.
    Jump(usize),
    Match,
}

pub struct Regex {
    states: Vec<State>,
    start: usize,
}

/// Builds NFA fragments; each returns (entry state, list of dangling exits to patch).
struct Builder {
    states: Vec<State>,
}

const HOLE: usize = usize::MAX;

impl Builder {
    fn push(&mut self, state: State) -> usize {
        self.states.push(state);
        self.states.len() - 1
    }

    fn patch(&mut self, holes: &[usize], target: usize) {
        for &h in holes {
            match &mut self.states[h] {
                State::Char(_, next) | State::Jump(next) => *next = target,
                State::Split(a, b) => {
                    if *a == HOLE {
                        *a = target;
                    } else {
                        *b = target;
                    }
                }
                State::Match => unreachable!("Match has no exit"),
            }
        }
    }

    fn build(&mut self, node: &Node) -> (usize, Vec<usize>) {
        match node {
            Node::Empty => {
                let s = self.push(State::Jump(HOLE));
                (s, vec![s])
            }
            Node::Set(set) => {
                let s = self.push(State::Char(set.clone(), HOLE));
                (s, vec![s])
            }
            Node::Concat(items) => {
                let (entry, mut holes) = self.build(&items[0]);
                for item in &items[1..] {
                    let (next, next_holes) = self.build(item);
                    self.patch(&holes, next);
                    holes = next_holes;
                }
                (entry, holes)
            }
            Node::Alt(a, b) => {
                let (ea, mut ha) = self.build(a);
                let (eb, hb) = self.build(b);
                let s = self.push(State::Split(ea, eb));
                ha.extend(hb);
                (s, ha)
            }
            Node::Star(inner) => {
                let s = self.push(State::Split(HOLE, HOLE));
                let (e, holes) = self.build(inner);
                self.states[s] = State::Split(e, HOLE);
                self.patch(&holes, s);
                (s, vec![s])
            }
            Node::Plus(inner) => {
                let (e, holes) = self.build(inner);
                let s = self.push(State::Split(e, HOLE));
                self.patch(&holes, s);
                (e, vec![s])
            }
            Node::Optional(inner) => {
                let (e, mut holes) = self.build(inner);
                let s = self.push(State::Split(e, HOLE));
                holes.push(s);
                (s, holes)
            }
        }
    }
}

impl Regex {
    pub fn new(pattern: &str) -> Result<Regex, RegexError> {
        let mut parser = Parser { chars: pattern.chars().collect(), pos: 0 };
        let ast = parser.alternation()?;
        if parser.pos < parser.chars.len() {
            // the only way to stop early is an unmatched ')'
            return Err(RegexError::UnbalancedParen(parser.pos));
        }
        let mut builder = Builder { states: Vec::new() };
        let (start, holes) = builder.build(&ast);
        let accept = builder.push(State::Match);
        builder.patch(&holes, accept);
        Ok(Regex { states: builder.states, start })
    }

    /// The number of NFA states; it grows linearly with the pattern.
    pub fn state_count(&self) -> usize {
        self.states.len()
    }

    /// Adds `s` and everything reachable from it without consuming input.
    fn add(&self, set: &mut Vec<usize>, on: &mut [bool], s: usize) {
        let mut stack = vec![s];
        while let Some(s) = stack.pop() {
            if on[s] {
                continue;
            }
            on[s] = true;
            match &self.states[s] {
                State::Split(a, b) => {
                    stack.push(*b);
                    stack.push(*a);
                }
                State::Jump(next) => stack.push(*next),
                _ => set.push(s),
            }
        }
    }

    fn step(&self, current: &[usize], c: char) -> Vec<usize> {
        let mut next = Vec::new();
        let mut on = vec![false; self.states.len()];
        for &s in current {
            if let State::Char(set, to) = &self.states[s]
                && set.contains(c)
            {
                self.add(&mut next, &mut on, *to);
            }
        }
        next
    }

    fn start_set(&self) -> Vec<usize> {
        let mut set = Vec::new();
        let mut on = vec![false; self.states.len()];
        self.add(&mut set, &mut on, self.start);
        set
    }

    fn accepts(&self, set: &[usize]) -> bool {
        set.iter().any(|&s| matches!(self.states[s], State::Match))
    }

    /// Does the whole text match the pattern?
    pub fn is_match(&self, text: &str) -> bool {
        let mut current = self.start_set();
        for c in text.chars() {
            current = self.step(&current, c);
            if current.is_empty() {
                return false;
            }
        }
        self.accepts(&current)
    }

    /// The leftmost match in the text, the longest one at that position, as byte offsets.
    pub fn find(&self, text: &str) -> Option<(usize, usize)> {
        let starts: Vec<usize> = text.char_indices().map(|(i, _)| i).chain([text.len()]).collect();
        for &begin in &starts {
            let mut current = self.start_set();
            let mut best = self.accepts(&current).then_some(begin);
            for (offset, c) in text[begin..].char_indices() {
                current = self.step(&current, c);
                if current.is_empty() {
                    break;
                }
                if self.accepts(&current) {
                    best = Some(begin + offset + c.len_utf8());
                }
            }
            if let Some(end) = best {
                return Some((begin, end));
            }
        }
        None
    }
}

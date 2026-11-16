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

pub struct Regex {
    // Your compiled pattern: for example the states of a Thompson NFA and its start state.
    pattern: String,
}

impl Regex {
    pub fn new(pattern: &str) -> Result<Regex, RegexError> {
        let _ = Regex { pattern: pattern.to_string() };
        todo!("parse {pattern:?} and build the NFA")
    }

    /// The number of NFA states; it grows linearly with the pattern.
    pub fn state_count(&self) -> usize {
        todo!("count the states of {:?}", self.pattern)
    }

    /// Does the whole text match the pattern?
    pub fn is_match(&self, text: &str) -> bool {
        todo!("simulate the NFA on {text:?}")
    }

    /// The leftmost match in the text, the longest one at that position, as byte offsets.
    pub fn find(&self, text: &str) -> Option<(usize, usize)> {
        todo!("find {:?} in {text:?}", self.pattern)
    }
}

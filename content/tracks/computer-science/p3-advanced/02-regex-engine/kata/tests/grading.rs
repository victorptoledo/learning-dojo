use std::time::{Duration, Instant};

use kata::{Regex, RegexError};

fn re(pattern: &str) -> Regex {
    Regex::new(pattern).unwrap_or_else(|e| panic!("{pattern:?} should compile: {e:?}"))
}

fn check(pattern: &str, yes: &[&str], no: &[&str]) {
    let r = re(pattern);
    for text in yes {
        assert!(r.is_match(text), "{pattern:?} should match {text:?}");
    }
    for text in no {
        assert!(!r.is_match(text), "{pattern:?} should not match {text:?}");
    }
}

#[test]
fn literals_and_concatenation() {
    check("abc", &["abc"], &["", "ab", "abcd", "xabc", "ABC"]);
    check("", &[""], &["a"]);
}

#[test]
fn dot_matches_any_single_character() {
    check("a.c", &["abc", "a.c", "a c", "aéc", "a😀c"], &["ac", "abbc"]);
    check("...", &["abc", "日本語"], &["ab"]);
}

#[test]
fn quantifiers() {
    check("ab*c", &["ac", "abc", "abbbbc"], &["abbb", "bc"]);
    check("ab+c", &["abc", "abbbc"], &["ac"]);
    check("ab?c", &["ac", "abc"], &["abbc"]);
    check("a*", &["", "a", "aaaa"], &["b", "aab"]);
}

#[test]
fn alternation_and_groups() {
    check("cat|dog", &["cat", "dog"], &["catdog", "ca", ""]);
    check("gr(a|e)y", &["gray", "grey"], &["griy", "gry"]);
    check("(ab)+", &["ab", "abab"], &["", "aba", "abb"]);
    check("a(b|c)*d", &["ad", "abcbd", "accccd"], &["abc", "abed"]);
    check("(a|b)(c|d)(e|f)", &["ace", "bdf", "bcf"], &["ab", "aceg"]);
    check("((a))", &["a"], &["aa"]);
}

#[test]
fn empty_alternatives_and_groups() {
    check("a|", &["a", ""], &["b"]);
    check("|b", &["", "b"], &["a"]);
    check("x()y", &["xy"], &["x y"]);
    check("(|a)b", &["b", "ab"], &["aab"]);
    check("(a|)*", &["", "a", "aaa"], &["b"]);
}

#[test]
fn escapes_turn_metacharacters_into_literals() {
    check(r"a\.b", &["a.b"], &["axb"]);
    check(r"\(\)\*\+\?\|\\", &[r"()*+?|\"], &[""]);
    check(r"\[x\]", &["[x]"], &["x"]);
}

#[test]
fn character_classes() {
    check("[abc]+", &["a", "cab"], &["", "abd"]);
    check("[a-z0-9_]+", &["snake_case_2"], &["Snake", "a-b"]);
    check("[^0-9]*", &["", "abc", "é!"], &["a1"]);
    check("[-a]", &["-", "a"], &["b"]);
    check("[a-]", &["-", "a"], &["b"]);
    check(r"[\]\\]", &["]", "\\"], &["a"]);
    check("[.]", &["."], &["a"]);
    check("[α-ω]+", &["λογος"], &["logos"]);
}

#[test]
fn errors_name_the_position() {
    let cases = [
        ("*", RegexError::NothingToRepeat(0)),
        ("a|+b", RegexError::NothingToRepeat(2)),
        ("(?a)", RegexError::NothingToRepeat(1)),
        ("a**", RegexError::NothingToRepeat(2)),
        ("a+?", RegexError::NothingToRepeat(2)),
        ("(ab", RegexError::UnbalancedParen(0)),
        ("a(b(c)", RegexError::UnbalancedParen(1)),
        ("ab)", RegexError::UnbalancedParen(2)),
        ("())", RegexError::UnbalancedParen(2)),
        ("ab\\", RegexError::TrailingBackslash(2)),
        ("x[ab", RegexError::UnclosedClass(1)),
        ("[a\\", RegexError::UnclosedClass(0)),
        ("[]", RegexError::EmptyClass(0)),
        ("a[^]", RegexError::EmptyClass(1)),
        ("[az-a]", RegexError::BadRange(2)),
        ("éé*(", RegexError::UnbalancedParen(3)),
    ];
    for (pattern, expected) in cases {
        assert_eq!(Regex::new(pattern).err(), Some(expected), "pattern {pattern:?}");
    }
}

#[test]
fn find_returns_the_leftmost_longest_match_in_bytes() {
    assert_eq!(re("b+").find("aabbbcbb"), Some((2, 5)));
    assert_eq!(re("cat|category").find("my category"), Some((3, 11)));
    assert_eq!(re("x*").find("abc"), Some((0, 0)));
    assert_eq!(re("[0-9]+").find("no digits"), None);
    assert_eq!(re("é+").find("caféé!"), Some((3, 7)));
    assert_eq!(re("a|b").find(""), None);
    assert_eq!(re("").find(""), Some((0, 0)));
}

#[test]
fn the_nfa_grows_linearly_with_the_pattern() {
    for n in [1usize, 10, 100, 1000] {
        let pattern = "(a|b)*c?".repeat(n);
        let chars = pattern.chars().count();
        let states = re(&pattern).state_count();
        assert!(states <= 4 * chars + 4, "{states} states for {chars} pattern characters");
    }
}

fn timed<T>(limit: Duration, f: impl FnOnce() -> T) -> T {
    let start = Instant::now();
    let value = f();
    assert!(start.elapsed() < limit, "took {:?}", start.elapsed());
    value
}

#[test]
fn nested_stars_do_not_explode() {
    let text = "a".repeat(20_000);
    assert!(!timed(Duration::from_secs(5), || re("(a*)*b").is_match(&text)));
    assert!(timed(Duration::from_secs(5), || re("(a*)*").is_match(&text)));
    let xs = "x".repeat(5_000) + "!";
    assert!(!timed(Duration::from_secs(5), || re("(x+x+)+y").is_match(&xs)));
}

#[test]
fn the_classic_exponential_case_is_linear() {
    // a?ⁿaⁿ against aⁿ: a backtracking engine tries 2ⁿ paths
    let n = 40;
    let pattern = "a?".repeat(n) + &"a".repeat(n);
    let text = "a".repeat(n);
    assert!(timed(Duration::from_secs(5), || re(&pattern).is_match(&text)));
    assert!(!re(&pattern).is_match(&"a".repeat(n - 1)));
    assert!(re(&pattern).is_match(&"a".repeat(2 * n)));
    assert!(!re(&pattern).is_match(&"a".repeat(2 * n + 1)));
}

#[test]
fn long_texts_are_matched_quickly() {
    let r = re("([a-z]+ )*[a-z]+");
    let text = "lorem ipsum dolor sit amet ".repeat(4_000) + "end";
    assert!(timed(Duration::from_secs(5), || r.is_match(&text)));
    assert_eq!(
        timed(Duration::from_secs(5), || re("amet end").find(&text)),
        Some((text.len() - 8, text.len()))
    );
}

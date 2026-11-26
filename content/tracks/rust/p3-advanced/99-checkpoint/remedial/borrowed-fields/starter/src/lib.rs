use std::sync::Arc;

/// The trimmed, non-empty comma-separated fields of `line`, borrowed from it.
pub fn fields(line: &str) -> Vec<&str> {
    todo!("split {line:?} on commas")
}

/// The longer of `a` and `b` in characters; `a` on a tie.
pub fn longer<'a>(a: &'a str, b: &'a str) -> &'a str {
    todo!("compare {a:?} and {b:?}")
}

/// How many items pass `test`, checked on `workers` spawned threads.
pub fn count_matching(items: Vec<String>, workers: usize, test: Arc<dyn Fn(&str) -> bool + Send + Sync>) -> usize {
    todo!("count {} items on {workers} threads", items.len())
}

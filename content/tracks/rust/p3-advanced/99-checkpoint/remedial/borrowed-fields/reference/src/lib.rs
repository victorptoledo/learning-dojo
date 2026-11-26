use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::thread;

/// The trimmed, non-empty comma-separated fields of `line`, borrowed from it.
pub fn fields(line: &str) -> Vec<&str> {
    line.split(',').map(str::trim).filter(|field| !field.is_empty()).collect()
}

/// The longer of `a` and `b` in characters; `a` on a tie.
pub fn longer<'a>(a: &'a str, b: &'a str) -> &'a str {
    if b.chars().count() > a.chars().count() { b } else { a }
}

/// How many items pass `test`, checked on `workers` spawned threads.
pub fn count_matching(items: Vec<String>, workers: usize, test: Arc<dyn Fn(&str) -> bool + Send + Sync>) -> usize {
    let workers = workers.max(1);
    let total = Arc::new(AtomicUsize::new(0));
    let mut parts: Vec<Vec<String>> = (0..workers).map(|_| Vec::new()).collect();
    for (index, item) in items.into_iter().enumerate() {
        parts[index % workers].push(item);
    }
    let handles: Vec<_> = parts
        .into_iter()
        .map(|part| {
            let (test, total) = (Arc::clone(&test), Arc::clone(&total));
            thread::spawn(move || {
                let found = part.iter().filter(|item| test(item)).count();
                total.fetch_add(found, Ordering::SeqCst);
            })
        })
        .collect();
    for handle in handles {
        handle.join().unwrap();
    }
    total.load(Ordering::SeqCst)
}

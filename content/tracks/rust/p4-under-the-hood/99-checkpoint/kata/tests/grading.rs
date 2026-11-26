use kata::{FixedVec, fixed_vec};
use std::cell::Cell;
use std::panic::{AssertUnwindSafe, catch_unwind};
use std::rc::Rc;

/// Counts its own drops, so the tests can see leaks and double drops.
#[derive(Debug)]
struct Noisy {
    id: u32,
    drops: Rc<Cell<u32>>,
    /// `clone` panics on this id.
    poison: Option<u32>,
}

impl Noisy {
    fn new(id: u32, drops: &Rc<Cell<u32>>) -> Self {
        Noisy { id, drops: Rc::clone(drops), poison: None }
    }
}

impl Clone for Noisy {
    fn clone(&self) -> Self {
        if self.poison == Some(self.id) {
            panic!("clone of {} fails", self.id);
        }
        Noisy { id: self.id, drops: Rc::clone(&self.drops), poison: self.poison }
    }
}

impl Drop for Noisy {
    fn drop(&mut self) {
        self.drops.set(self.drops.get() + 1);
    }
}

fn counter() -> Rc<Cell<u32>> {
    Rc::new(Cell::new(0))
}

fn ids<const N: usize>(vec: &FixedVec<Noisy, N>) -> Vec<u32> {
    vec.iter().map(|noisy| noisy.id).collect()
}

fn filled<const N: usize>(drops: &Rc<Cell<u32>>, count: u32) -> FixedVec<Noisy, N> {
    let mut vec = FixedVec::new();
    for id in 0..count {
        assert!(vec.push(Noisy::new(id, drops)).is_ok());
    }
    vec
}

// ---- push, pop and capacity ---------------------------------------------------------------

#[test]
fn the_capacity_comes_from_the_type() {
    let vec: FixedVec<u8, 16> = FixedVec::new();
    assert_eq!(vec.capacity(), 16);
    assert_eq!(vec.len(), 0);
    assert!(vec.is_empty() && !vec.is_full());
}

#[test]
fn pushes_until_full_and_gives_the_extra_value_back() {
    let drops = counter();
    let mut vec: FixedVec<Noisy, 2> = filled(&drops, 2);
    assert!(vec.is_full());
    let rejected = vec.push(Noisy::new(9, &drops)).unwrap_err();
    assert_eq!(rejected.id, 9);
    assert_eq!(drops.get(), 0, "a value was dropped while pushing");
    assert_eq!(ids(&vec), [0, 1]);
}

#[test]
fn zero_capacity_takes_nothing() {
    let mut vec: FixedVec<i32, 0> = FixedVec::new();
    assert_eq!(vec.push(1), Err(1));
    assert_eq!(vec.pop(), None);
}

#[test]
fn pop_returns_the_last_value_without_dropping_it() {
    let drops = counter();
    let mut vec: FixedVec<Noisy, 4> = filled(&drops, 3);
    let last = vec.pop().unwrap();
    assert_eq!(last.id, 2);
    assert_eq!(drops.get(), 0);
    assert_eq!(vec.len(), 2);
    drop(last);
    assert_eq!(drops.get(), 1);
    assert!(vec.pop().is_some() && vec.pop().is_some() && vec.pop().is_none());
}

// ---- dropping -----------------------------------------------------------------------------

#[test]
fn dropping_the_vector_drops_every_value_exactly_once() {
    let drops = counter();
    let vec: FixedVec<Noisy, 8> = filled(&drops, 5);
    drop(vec);
    assert_eq!(drops.get(), 5);
}

#[test]
fn a_partly_used_vector_drops_only_its_values() {
    let drops = counter();
    let mut vec: FixedVec<Noisy, 8> = filled(&drops, 4);
    drop(vec.pop());
    drop(vec);
    assert_eq!(drops.get(), 4);
}

#[test]
fn clear_drops_the_values_and_keeps_the_room() {
    let drops = counter();
    let mut vec: FixedVec<Noisy, 3> = filled(&drops, 3);
    vec.clear();
    assert_eq!(drops.get(), 3);
    assert!(vec.is_empty());
    assert!(vec.push(Noisy::new(7, &drops)).is_ok());
    drop(vec);
    assert_eq!(drops.get(), 4);
}

// ---- insert and remove --------------------------------------------------------------------

#[test]
fn insert_shifts_the_rest_to_the_right() {
    let mut vec: FixedVec<i32, 5> = fixed_vec![1, 2, 3];
    vec.insert(1, 9).unwrap();
    assert_eq!(vec.as_slice(), [1, 9, 2, 3]);
    vec.insert(0, 0).unwrap();
    assert_eq!(vec.as_slice(), [0, 1, 9, 2, 3]);
    assert_eq!(vec.insert(5, 4), Err(4));
}

#[test]
fn insert_at_the_end_appends() {
    let mut vec: FixedVec<i32, 3> = fixed_vec![1];
    vec.insert(1, 2).unwrap();
    assert_eq!(vec.as_slice(), [1, 2]);
}

#[test]
fn insert_past_the_end_panics() {
    let mut vec: FixedVec<i32, 4> = fixed_vec![1, 2];
    assert!(panic_message(move || drop(vec.insert(3, 7))).is_some(), "insert(3, ..) on a vector of 2 must panic");
}

#[test]
fn remove_shifts_the_rest_to_the_left_and_returns_the_value() {
    let drops = counter();
    let mut vec: FixedVec<Noisy, 4> = filled(&drops, 4);
    let removed = vec.remove(1);
    assert_eq!(removed.id, 1);
    assert_eq!(ids(&vec), [0, 2, 3]);
    assert_eq!(vec.remove(2).id, 3);
    assert_eq!(ids(&vec), [0, 2]);
    drop(removed);
    drop(vec);
    assert_eq!(drops.get(), 4, "every value must be dropped exactly once");
}

#[test]
fn remove_past_the_end_panics() {
    let mut vec: FixedVec<i32, 4> = fixed_vec![1, 2];
    assert!(panic_message(move || drop(vec.remove(2))).is_some(), "remove(2) on a vector of 2 must panic");
}

#[test]
fn insert_and_remove_keep_every_value_alive_once() {
    let drops = counter();
    let mut vec: FixedVec<Noisy, 6> = filled(&drops, 3);
    vec.insert(0, Noisy::new(10, &drops)).unwrap();
    vec.insert(2, Noisy::new(11, &drops)).unwrap();
    assert_eq!(ids(&vec), [10, 0, 11, 1, 2]);
    drop(vec.remove(0));
    drop(vec.remove(3));
    assert_eq!(ids(&vec), [0, 11, 1]);
    assert_eq!(drops.get(), 2);
    drop(vec);
    assert_eq!(drops.get(), 5);
}

// ---- traits -------------------------------------------------------------------------------

#[test]
fn derefs_to_a_slice() {
    let mut vec: FixedVec<i32, 8> = fixed_vec![5, 3, 9, 1];
    assert_eq!(vec[2], 9);
    assert_eq!(vec.iter().sum::<i32>(), 18);
    assert!(vec.contains(&3));
    vec.sort();
    vec[0] = 0;
    assert_eq!(vec.as_slice(), [0, 3, 5, 9]);
    assert_eq!(vec.first(), Some(&0));
}

#[test]
fn debug_prints_only_the_values() {
    let vec: FixedVec<i32, 8> = fixed_vec![1, 2, 3];
    assert_eq!(format!("{vec:?}"), "[1, 2, 3]");
    let empty: FixedVec<i32, 2> = FixedVec::new();
    assert_eq!(format!("{empty:?}"), "[]");
}

#[test]
fn vectors_with_the_same_values_are_equal_whatever_their_capacity() {
    let small: FixedVec<i32, 3> = fixed_vec![1, 2];
    let large: FixedVec<i32, 10> = fixed_vec![1, 2];
    let other: FixedVec<i32, 3> = fixed_vec![2, 1];
    assert!(small == large);
    assert!(small != other);
}

#[test]
fn a_clone_owns_its_own_values() {
    let drops = counter();
    let vec: FixedVec<Noisy, 4> = filled(&drops, 3);
    let copy = vec.clone();
    assert_eq!(ids(&copy), [0, 1, 2]);
    drop(vec);
    assert_eq!(drops.get(), 3);
    assert_eq!(ids(&copy), [0, 1, 2]);
    drop(copy);
    assert_eq!(drops.get(), 6);
}

#[test]
fn a_clone_that_panics_halfway_neither_leaks_nor_double_drops() {
    let drops = counter();
    let mut vec: FixedVec<Noisy, 4> = FixedVec::new();
    for id in 0..4 {
        let mut noisy = Noisy::new(id, &drops);
        noisy.poison = Some(2);
        assert!(vec.push(noisy).is_ok());
    }
    let result = catch_unwind(AssertUnwindSafe(|| vec.clone()));
    assert!(result.is_err());
    assert_eq!(drops.get(), 2, "the two values cloned before the panic must be dropped");
    drop(vec);
    assert_eq!(drops.get(), 6);
}

#[test]
fn loops_over_a_reference() {
    let vec: FixedVec<&str, 4> = fixed_vec!["a", "b", "c"];
    let mut joined = String::new();
    for letter in &vec {
        joined.push_str(letter);
    }
    assert_eq!(joined, "abc");
    assert_eq!(vec.len(), 3);
}

#[test]
fn into_iter_moves_the_values_out_in_order() {
    let vec: FixedVec<String, 4> = fixed_vec!["x".to_string(), "y".to_string()];
    let mut iter = vec.into_iter();
    assert_eq!(iter.len(), 2);
    let all: Vec<String> = iter.by_ref().collect();
    assert_eq!(all, ["x", "y"]);
    assert_eq!(iter.next(), None);
}

#[test]
fn dropping_a_half_used_iterator_drops_the_rest() {
    let drops = counter();
    let vec: FixedVec<Noisy, 8> = filled(&drops, 5);
    let mut iter = vec.into_iter();
    let first = iter.next().unwrap();
    assert_eq!(first.id, 0);
    assert_eq!(iter.size_hint(), (4, Some(4)));
    drop(iter);
    assert_eq!(drops.get(), 4);
    drop(first);
    assert_eq!(drops.get(), 5);
}

// ---- the macro ----------------------------------------------------------------------------

#[test]
fn the_macro_builds_lists_with_or_without_a_trailing_comma() {
    let a: FixedVec<i32, 4> = fixed_vec![1, 2, 3];
    let b: FixedVec<i32, 4> = fixed_vec![1, 2, 3,];
    let empty: FixedVec<i32, 4> = fixed_vec![];
    assert_eq!(a.as_slice(), [1, 2, 3]);
    assert!(a == b);
    assert!(empty.is_empty());
}

#[test]
fn the_macro_repeats_a_value() {
    let vec: FixedVec<String, 5> = fixed_vec![String::from("ab"); 3];
    assert_eq!(vec.as_slice(), ["ab", "ab", "ab"]);
    let none: FixedVec<String, 5> = fixed_vec![String::from("ab"); 0];
    assert!(none.is_empty());
}

#[test]
fn the_macro_evaluates_each_argument_once() {
    let calls = Cell::new(0);
    let next = || {
        calls.set(calls.get() + 1);
        calls.get()
    };
    let vec: FixedVec<i32, 4> = fixed_vec![next(), next()];
    assert_eq!(vec.as_slice(), [1, 2]);
    let repeated: FixedVec<i32, 4> = fixed_vec![next(); 3];
    assert_eq!(repeated.as_slice(), [3, 3, 3]);
    assert_eq!(calls.get(), 3);
}

#[test]
fn the_macro_panics_when_the_values_do_not_fit() {
    let message = panic_message(|| {
        let _vec: FixedVec<i32, 2> = fixed_vec![1, 2, 3];
    });
    assert!(message.as_deref().is_some_and(|m| m.contains("capacity")), "panic message: {message:?}");
}

#[test]
fn the_repeat_form_panics_when_the_values_do_not_fit() {
    let message = panic_message(|| {
        let _vec: FixedVec<i32, 2> = fixed_vec![0; 3];
    });
    assert!(message.as_deref().is_some_and(|m| m.contains("capacity")), "panic message: {message:?}");
}

/// Runs `f` and returns its panic message, or `None` if it did not panic.
fn panic_message(f: impl FnOnce()) -> Option<String> {
    let payload = catch_unwind(AssertUnwindSafe(f)).err()?;
    Some(
        payload
            .downcast_ref::<String>()
            .cloned()
            .or_else(|| payload.downcast_ref::<&str>().map(|s| s.to_string()))
            .unwrap_or_default(),
    )
}

mod elsewhere {
    // The macro must work where FixedVec is not imported: it should name `$crate::FixedVec`.
    #[test]
    fn the_macro_works_without_importing_the_type() {
        let vec: kata::FixedVec<u8, 2> = kata::fixed_vec![1, 2];
        assert_eq!(vec.len(), 2);
    }
}

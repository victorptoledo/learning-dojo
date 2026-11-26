use kata::{Meters, TinyStack, max_of};
use std::cell::Cell;
use std::rc::Rc;

#[test]
fn meters_have_the_four_operators() {
    assert_eq!(Meters(1.5) + Meters(2.0), Meters(3.5));
    assert_eq!(Meters(5.0) - Meters(1.5), Meters(3.5));
    assert_eq!(-Meters(2.0), Meters(-2.0));
    assert_eq!(Meters(1.5) * 2.0, Meters(3.0));
}

#[test]
fn meters_print_with_a_unit() {
    assert_eq!(Meters(3.5).to_string(), "3.5 m");
    assert_eq!(format!("{}", Meters(-1.0)), "-1 m");
}

#[test]
fn max_of_one_or_many() {
    assert_eq!(max_of!(7), 7);
    assert_eq!(max_of!(3, 9, 4), 9);
    assert_eq!(max_of!(1, 2, 3, 4, 5, 2,), 5);
    assert_eq!(max_of!("pear", "apple"), "pear");
}

#[test]
fn max_of_keeps_the_first_on_a_tie() {
    let (first, second) = ((2, "first"), (2, "second"));
    let picked = max_of!(first.0, second.0);
    assert_eq!(picked, 2);
    // compare (value, label) pairs by value only through a wrapper
    #[derive(Debug, PartialEq)]
    struct ByValue(i32, &'static str);
    impl PartialOrd for ByValue {
        fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
            self.0.partial_cmp(&other.0)
        }
    }
    assert_eq!(max_of!(ByValue(first.0, first.1), ByValue(second.0, second.1)).1, "first");
}

#[test]
fn max_of_evaluates_each_expression_once() {
    let calls = Cell::new(0);
    let next = |value: i32| {
        calls.set(calls.get() + 1);
        value
    };
    assert_eq!(max_of!(next(1), next(5), next(3)), 5);
    assert_eq!(calls.get(), 3);
}

struct Noisy(Rc<Cell<u32>>);

impl Drop for Noisy {
    fn drop(&mut self) {
        self.0.set(self.0.get() + 1);
    }
}

#[test]
fn the_stack_is_last_in_first_out() {
    let mut stack: TinyStack<i32, 3> = TinyStack::new();
    for n in 1..=3 {
        assert_eq!(stack.push(n), Ok(()));
    }
    assert_eq!(stack.push(4), Err(4));
    assert_eq!(stack.peek(), Some(&3));
    assert_eq!(stack.len(), 3);
    assert_eq!(stack.pop(), Some(3));
    assert_eq!(stack.pop(), Some(2));
    assert_eq!(stack.len(), 1);
}

#[test]
fn an_empty_stack_has_nothing_to_give() {
    let mut stack: TinyStack<String, 2> = TinyStack::new();
    assert_eq!(stack.peek(), None);
    assert_eq!(stack.pop(), None);
    let mut none: TinyStack<u8, 0> = TinyStack::new();
    assert_eq!(none.push(1), Err(1));
}

#[test]
fn dropping_the_stack_drops_each_value_once() {
    let drops = Rc::new(Cell::new(0));
    let mut stack: TinyStack<Noisy, 4> = TinyStack::new();
    for _ in 0..3 {
        assert!(stack.push(Noisy(Rc::clone(&drops))).is_ok());
    }
    let top = stack.pop().unwrap();
    assert_eq!(drops.get(), 0);
    drop(stack);
    assert_eq!(drops.get(), 2);
    drop(top);
    assert_eq!(drops.get(), 3);
}

#[test]
fn a_rejected_value_is_given_back_alive() {
    let drops = Rc::new(Cell::new(0));
    let mut stack: TinyStack<Noisy, 1> = TinyStack::new();
    assert!(stack.push(Noisy(Rc::clone(&drops))).is_ok());
    let back = stack.push(Noisy(Rc::clone(&drops))).unwrap_err();
    assert_eq!(drops.get(), 0);
    drop(back);
    drop(stack);
    assert_eq!(drops.get(), 2);
}

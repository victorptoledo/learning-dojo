use kata::{Meters, TinyStack, max_of};

#[test]
fn adds_meters() {
    assert_eq!(Meters(1.5) + Meters(2.0), Meters(3.5));
}

#[test]
fn finds_the_max() {
    assert_eq!(max_of!(3, 9, 4), 9);
}

#[test]
fn pushes_and_pops() {
    let mut stack: TinyStack<i32, 2> = TinyStack::new();
    assert_eq!(stack.push(1), Ok(()));
    assert_eq!(stack.pop(), Some(1));
}

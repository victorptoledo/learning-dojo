use kata::{FixedVec, fixed_sum, fixed_vec};

#[test]
fn pushes_and_pops() {
    let mut vec: FixedVec<i32, 2> = FixedVec::new();
    assert_eq!(vec.push(1), Ok(()));
    assert_eq!(vec.push(2), Ok(()));
    assert_eq!(vec.push(3), Err(3));
    assert_eq!(vec.pop(), Some(2));
    assert_eq!(vec.len(), 1);
}

#[test]
fn behaves_like_a_slice() {
    let mut vec: FixedVec<i32, 4> = fixed_vec![3, 1, 2];
    vec.sort();
    assert_eq!(vec[0], 1);
    assert_eq!(format!("{vec:?}"), "[1, 2, 3]");
}

#[test]
fn sums_through_the_c_function() {
    let values = [1i64, 2, 3];
    // SAFETY: the pointer and length come from a live array.
    assert_eq!(unsafe { fixed_sum(values.as_ptr(), values.len()) }, 6);
}

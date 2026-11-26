// The C-facing function lives in its own test binary: a panic inside an `extern "C"` function
// aborts the process, and that should only take these tests down.
use kata::fixed_sum;

#[test]
fn sums_a_buffer_passed_as_a_pointer_and_a_length() {
    let values = [3i64, 4, -2];
    // SAFETY: the pointer and length come from a live array.
    assert_eq!(unsafe { fixed_sum(values.as_ptr(), values.len()) }, 5);
}

#[test]
fn a_null_pointer_or_an_empty_buffer_sums_to_zero() {
    // SAFETY: null is allowed by the contract.
    assert_eq!(unsafe { fixed_sum(std::ptr::null(), 10) }, 0);
    let values: [i64; 0] = [];
    // SAFETY: an empty array with its own length.
    assert_eq!(unsafe { fixed_sum(values.as_ptr(), 0) }, 0);
}

#[test]
fn overflow_wraps_instead_of_panicking_across_the_c_boundary() {
    let values = [i64::MAX, 1, 1];
    // SAFETY: the pointer and length come from a live array.
    assert_eq!(unsafe { fixed_sum(values.as_ptr(), values.len()) }, i64::MIN + 1);
}

#[test]
fn reads_only_len_values() {
    let values = [1i64, 2, 3, 100];
    // SAFETY: 3 is within the array.
    assert_eq!(unsafe { fixed_sum(values.as_ptr(), 3) }, 6);
}

#[test]
fn keeps_its_c_signature() {
    let f: unsafe extern "C" fn(*const i64, usize) -> i64 = fixed_sum;
    let values = [7i64];
    // SAFETY: as above.
    assert_eq!(unsafe { f(values.as_ptr(), 1) }, 7);
}

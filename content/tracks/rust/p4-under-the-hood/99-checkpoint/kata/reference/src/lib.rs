//! Checkpoint 4: FixedVec (reference solution).

use std::fmt;
use std::mem::{ManuallyDrop, MaybeUninit};
use std::ops::{Deref, DerefMut};
use std::ptr;

/// A vector with room for exactly `N` values, stored inline (no heap allocation).
///
/// Invariant: `slots[..len]` are initialised, `slots[len..]` are not.
pub struct FixedVec<T, const N: usize> {
    slots: [MaybeUninit<T>; N],
    len: usize,
}

impl<T, const N: usize> FixedVec<T, N> {
    pub const fn new() -> Self {
        FixedVec { slots: [const { MaybeUninit::uninit() }; N], len: 0 }
    }

    pub const fn capacity(&self) -> usize {
        N
    }

    pub fn is_full(&self) -> bool {
        self.len == N
    }

    /// Adds `value` at the end, or gives it back when there is no room.
    pub fn push(&mut self, value: T) -> Result<(), T> {
        if self.is_full() {
            return Err(value);
        }
        self.slots[self.len].write(value);
        self.len += 1;
        Ok(())
    }

    pub fn pop(&mut self) -> Option<T> {
        if self.len == 0 {
            return None;
        }
        self.len -= 1;
        // SAFETY: the slot was below len, so it is initialised; len no longer covers it.
        Some(unsafe { self.slots[self.len].assume_init_read() })
    }

    /// Inserts `value` at `index`, shifting the rest to the right.
    /// Panics if `index > len`; gives the value back when full.
    pub fn insert(&mut self, index: usize, value: T) -> Result<(), T> {
        assert!(index <= self.len, "insertion index {index} is out of bounds (len {})", self.len);
        if self.is_full() {
            return Err(value);
        }
        let base = self.slots.as_mut_ptr().cast::<T>();
        // SAFETY: index <= len < N, so both ranges are inside the array; ptr::copy allows overlap.
        // The hole at index is then filled before len grows.
        unsafe {
            ptr::copy(base.add(index), base.add(index + 1), self.len - index);
            base.add(index).write(value);
        }
        self.len += 1;
        Ok(())
    }

    /// Removes and returns the value at `index`, shifting the rest to the left.
    /// Panics if `index >= len`.
    pub fn remove(&mut self, index: usize) -> T {
        assert!(index < self.len, "removal index {index} is out of bounds (len {})", self.len);
        let base = self.slots.as_mut_ptr().cast::<T>();
        // SAFETY: index < len, so the slot is initialised. After reading it, the values above it
        // move down one place and len shrinks, so no slot is dropped twice.
        unsafe {
            let value = base.add(index).read();
            ptr::copy(base.add(index + 1), base.add(index), self.len - index - 1);
            self.len -= 1;
            value
        }
    }

    pub fn clear(&mut self) {
        let len = self.len;
        // Set len first: if a destructor panics, the vector does not drop those values again.
        self.len = 0;
        // SAFETY: the first len slots were initialised and are no longer covered by len.
        unsafe {
            ptr::drop_in_place(ptr::slice_from_raw_parts_mut(
                self.slots.as_mut_ptr().cast::<T>(),
                len,
            ));
        }
    }

    pub fn as_slice(&self) -> &[T] {
        // SAFETY: slots[..len] are initialised and MaybeUninit<T> has the layout of T.
        unsafe { std::slice::from_raw_parts(self.slots.as_ptr().cast::<T>(), self.len) }
    }

    pub fn as_mut_slice(&mut self) -> &mut [T] {
        // SAFETY: as above, and &mut self guarantees the borrow is unique.
        unsafe { std::slice::from_raw_parts_mut(self.slots.as_mut_ptr().cast::<T>(), self.len) }
    }
}

impl<T, const N: usize> Default for FixedVec<T, N> {
    fn default() -> Self {
        Self::new()
    }
}

impl<T, const N: usize> Drop for FixedVec<T, N> {
    fn drop(&mut self) {
        self.clear();
    }
}

impl<T, const N: usize> Deref for FixedVec<T, N> {
    type Target = [T];
    fn deref(&self) -> &[T] {
        self.as_slice()
    }
}

impl<T, const N: usize> DerefMut for FixedVec<T, N> {
    fn deref_mut(&mut self) -> &mut [T] {
        self.as_mut_slice()
    }
}

impl<T: Clone, const N: usize> Clone for FixedVec<T, N> {
    fn clone(&self) -> Self {
        let mut copy = FixedVec::new();
        for value in self.iter() {
            // Pushing one by one keeps len right: if a clone panics, `copy` drops what it holds.
            let _ = copy.push(value.clone());
        }
        copy
    }
}

impl<T: fmt::Debug, const N: usize> fmt::Debug for FixedVec<T, N> {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_list().entries(self.iter()).finish()
    }
}

impl<T: PartialEq, const N: usize, const M: usize> PartialEq<FixedVec<T, M>> for FixedVec<T, N> {
    fn eq(&self, other: &FixedVec<T, M>) -> bool {
        self.as_slice() == other.as_slice()
    }
}

impl<'a, T, const N: usize> IntoIterator for &'a FixedVec<T, N> {
    type Item = &'a T;
    type IntoIter = std::slice::Iter<'a, T>;
    fn into_iter(self) -> Self::IntoIter {
        self.iter()
    }
}

impl<T, const N: usize> IntoIterator for FixedVec<T, N> {
    type Item = T;
    type IntoIter = IntoIter<T, N>;
    fn into_iter(self) -> IntoIter<T, N> {
        let vec = ManuallyDrop::new(self);
        // SAFETY: `vec` is never dropped, so its slots move into the iterator exactly once.
        let slots = unsafe { ptr::read(&vec.slots) };
        IntoIter { slots, next: 0, end: vec.len }
    }
}

/// The owning iterator of a `FixedVec`.
///
/// Invariant: `slots[next..end]` are initialised and not yet yielded.
pub struct IntoIter<T, const N: usize> {
    slots: [MaybeUninit<T>; N],
    next: usize,
    end: usize,
}

impl<T, const N: usize> Iterator for IntoIter<T, N> {
    type Item = T;

    fn next(&mut self) -> Option<T> {
        if self.next == self.end {
            return None;
        }
        self.next += 1;
        // SAFETY: the slot was in next..end, and next has moved past it.
        Some(unsafe { self.slots[self.next - 1].assume_init_read() })
    }

    fn size_hint(&self) -> (usize, Option<usize>) {
        let left = self.end - self.next;
        (left, Some(left))
    }
}

impl<T, const N: usize> ExactSizeIterator for IntoIter<T, N> {}

impl<T, const N: usize> Drop for IntoIter<T, N> {
    fn drop(&mut self) {
        for _ in self.by_ref() {}
    }
}

/// Builds a `FixedVec`: `fixed_vec![]`, `fixed_vec![a, b, c]` or `fixed_vec![value; count]`.
/// Panics when the values do not fit.
#[macro_export]
macro_rules! fixed_vec {
    () => {
        $crate::FixedVec::new()
    };
    ( $value:expr ; $count:expr ) => {{
        let value = $value;
        let mut vec = $crate::FixedVec::new();
        for _ in 0..$count {
            if vec.push(::std::clone::Clone::clone(&value)).is_err() {
                panic!("fixed_vec!: more values than capacity");
            }
        }
        vec
    }};
    ( $( $item:expr ),+ $(,)? ) => {{
        let mut vec = $crate::FixedVec::new();
        $(
            if vec.push($item).is_err() {
                panic!("fixed_vec!: more values than capacity");
            }
        )+
        vec
    }};
}

/// Sums `len` numbers starting at `data`, wrapping on overflow. A null pointer sums to 0.
///
/// # Safety
/// Unless it is null, `data` must point to `len` initialised `i64` values that stay valid
/// during the call.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn fixed_sum(data: *const i64, len: usize) -> i64 {
    if data.is_null() {
        return 0;
    }
    // SAFETY: the caller promises data points to len valid values.
    let values = unsafe { std::slice::from_raw_parts(data, len) };
    values.iter().fold(0i64, |total, value| total.wrapping_add(*value))
}

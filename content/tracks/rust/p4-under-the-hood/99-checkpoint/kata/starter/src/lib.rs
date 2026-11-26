//! Checkpoint 4: FixedVec. See README.md for the rules.
#![allow(unused)]

use std::fmt;
use std::mem::MaybeUninit;
use std::ops::{Deref, DerefMut};

/// A vector with room for exactly `N` values, stored inline (no heap allocation).
///
/// Invariant: `slots[..len]` are initialised, `slots[len..]` are not.
/// Every unsafe block you write should say, in a `// SAFETY:` comment, why it keeps it.
pub struct FixedVec<T, const N: usize> {
    slots: [MaybeUninit<T>; N],
    len: usize,
}

impl<T, const N: usize> FixedVec<T, N> {
    pub const fn new() -> Self {
        // every slot starts uninitialised: [const { MaybeUninit::uninit() }; N]
        todo!()
    }

    pub const fn capacity(&self) -> usize {
        todo!()
    }

    pub fn is_full(&self) -> bool {
        todo!()
    }

    /// Adds `value` at the end, or gives it back when there is no room.
    pub fn push(&mut self, value: T) -> Result<(), T> {
        todo!()
    }

    pub fn pop(&mut self) -> Option<T> {
        todo!()
    }

    /// Inserts `value` at `index`, shifting the rest to the right.
    /// Panics if `index > len`; gives the value back when full.
    pub fn insert(&mut self, index: usize, value: T) -> Result<(), T> {
        todo!()
    }

    /// Removes and returns the value at `index`, shifting the rest to the left.
    /// Panics if `index >= len`.
    pub fn remove(&mut self, index: usize) -> T {
        todo!()
    }

    pub fn clear(&mut self) {
        todo!()
    }

    pub fn as_slice(&self) -> &[T] {
        todo!()
    }

    pub fn as_mut_slice(&mut self) -> &mut [T] {
        todo!()
    }
}

impl<T, const N: usize> Default for FixedVec<T, N> {
    fn default() -> Self {
        Self::new()
    }
}

impl<T, const N: usize> Drop for FixedVec<T, N> {
    fn drop(&mut self) {
        // TODO: MaybeUninit never drops what it holds, so drop the initialised values here.
    }
}

impl<T, const N: usize> Deref for FixedVec<T, N> {
    type Target = [T];
    fn deref(&self) -> &[T] {
        todo!()
    }
}

impl<T, const N: usize> DerefMut for FixedVec<T, N> {
    fn deref_mut(&mut self) -> &mut [T] {
        todo!()
    }
}

impl<T: Clone, const N: usize> Clone for FixedVec<T, N> {
    fn clone(&self) -> Self {
        todo!("a deep copy, safe even if a clone panics halfway")
    }
}

impl<T: fmt::Debug, const N: usize> fmt::Debug for FixedVec<T, N> {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        todo!("print like a list: [1, 2, 3]")
    }
}

impl<T: PartialEq, const N: usize, const M: usize> PartialEq<FixedVec<T, M>> for FixedVec<T, N> {
    fn eq(&self, other: &FixedVec<T, M>) -> bool {
        todo!()
    }
}

impl<'a, T, const N: usize> IntoIterator for &'a FixedVec<T, N> {
    type Item = &'a T;
    type IntoIter = std::slice::Iter<'a, T>;
    fn into_iter(self) -> Self::IntoIter {
        todo!()
    }
}

impl<T, const N: usize> IntoIterator for FixedVec<T, N> {
    type Item = T;
    type IntoIter = IntoIter<T, N>;
    fn into_iter(self) -> IntoIter<T, N> {
        todo!("move the slots out without running FixedVec's Drop")
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
        todo!()
    }

    fn size_hint(&self) -> (usize, Option<usize>) {
        todo!()
    }
}

impl<T, const N: usize> ExactSizeIterator for IntoIter<T, N> {}

impl<T, const N: usize> Drop for IntoIter<T, N> {
    fn drop(&mut self) {
        // TODO: drop the values that were never yielded.
    }
}

/// Builds a `FixedVec`: `fixed_vec![]`, `fixed_vec![a, b, c]` or `fixed_vec![value; count]`.
/// Panics when the values do not fit.
#[macro_export]
macro_rules! fixed_vec {
    () => {
        todo!("an empty FixedVec")
    };
    ( $value:expr ; $count:expr ) => {
        todo!("count clones of value")
    };
    ( $( $item:expr ),+ $(,)? ) => {
        todo!("push each item, panicking when it does not fit")
    };
}

/// Sums `len` numbers starting at `data`, wrapping on overflow. A null pointer sums to 0.
///
/// # Safety
/// Unless it is null, `data` must point to `len` initialised `i64` values that stay valid
/// during the call.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn fixed_sum(data: *const i64, len: usize) -> i64 {
    todo!()
}

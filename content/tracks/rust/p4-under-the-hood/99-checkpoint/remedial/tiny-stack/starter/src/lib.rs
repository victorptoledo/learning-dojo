#![allow(unused)]
use std::fmt;
use std::mem::MaybeUninit;
use std::ops::{Add, Mul, Neg, Sub};

/// A length in meters.
#[derive(Debug, Clone, Copy, PartialEq, PartialOrd)]
pub struct Meters(pub f64);

impl Add for Meters {
    type Output = Meters;
    fn add(self, other: Meters) -> Meters {
        todo!()
    }
}

impl Sub for Meters {
    type Output = Meters;
    fn sub(self, other: Meters) -> Meters {
        todo!()
    }
}

impl Neg for Meters {
    type Output = Meters;
    fn neg(self) -> Meters {
        todo!()
    }
}

impl Mul<f64> for Meters {
    type Output = Meters;
    fn mul(self, factor: f64) -> Meters {
        todo!()
    }
}

impl fmt::Display for Meters {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        todo!("print like 3.5 m")
    }
}

/// The largest of one or more expressions, each evaluated once.
#[macro_export]
macro_rules! max_of {
    // TODO: this placeholder returns the first expression and ignores the others.
    // Write one rule for a single expression and a recursive rule for several.
    ( $first:expr $( , $rest:expr )* $(,)? ) => {
        $first
    };
}

/// Up to `N` values on the stack. Invariant: `slots[..len]` are initialised.
pub struct TinyStack<T, const N: usize> {
    slots: [MaybeUninit<T>; N],
    len: usize,
}

impl<T, const N: usize> TinyStack<T, N> {
    pub fn new() -> Self {
        todo!()
    }

    pub fn len(&self) -> usize {
        todo!()
    }

    pub fn push(&mut self, value: T) -> Result<(), T> {
        todo!()
    }

    pub fn pop(&mut self) -> Option<T> {
        todo!()
    }

    pub fn peek(&self) -> Option<&T> {
        todo!()
    }
}

impl<T, const N: usize> Drop for TinyStack<T, N> {
    fn drop(&mut self) {
        // TODO: drop the values still on the stack
    }
}

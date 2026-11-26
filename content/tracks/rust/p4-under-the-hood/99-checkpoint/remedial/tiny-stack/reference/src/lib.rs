use std::fmt;
use std::mem::MaybeUninit;
use std::ops::{Add, Mul, Neg, Sub};

/// A length in meters.
#[derive(Debug, Clone, Copy, PartialEq, PartialOrd)]
pub struct Meters(pub f64);

impl Add for Meters {
    type Output = Meters;
    fn add(self, other: Meters) -> Meters {
        Meters(self.0 + other.0)
    }
}

impl Sub for Meters {
    type Output = Meters;
    fn sub(self, other: Meters) -> Meters {
        Meters(self.0 - other.0)
    }
}

impl Neg for Meters {
    type Output = Meters;
    fn neg(self) -> Meters {
        Meters(-self.0)
    }
}

impl Mul<f64> for Meters {
    type Output = Meters;
    fn mul(self, factor: f64) -> Meters {
        Meters(self.0 * factor)
    }
}

impl fmt::Display for Meters {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{} m", self.0)
    }
}

/// The largest of one or more expressions, each evaluated once.
#[macro_export]
macro_rules! max_of {
    ( $x:expr $(,)? ) => { $x };
    ( $x:expr, $( $rest:expr ),+ $(,)? ) => {{
        let first = $x;
        let rest = $crate::max_of!( $( $rest ),+ );
        if rest > first { rest } else { first }
    }};
}

/// Up to `N` values on the stack. Invariant: `slots[..len]` are initialised.
pub struct TinyStack<T, const N: usize> {
    slots: [MaybeUninit<T>; N],
    len: usize,
}

impl<T, const N: usize> TinyStack<T, N> {
    pub fn new() -> Self {
        TinyStack { slots: [const { MaybeUninit::uninit() }; N], len: 0 }
    }

    pub fn len(&self) -> usize {
        self.len
    }

    pub fn push(&mut self, value: T) -> Result<(), T> {
        if self.len == N {
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

    pub fn peek(&self) -> Option<&T> {
        let top = self.len.checked_sub(1)?;
        // SAFETY: top < len, so the slot is initialised.
        Some(unsafe { self.slots[top].assume_init_ref() })
    }
}

impl<T, const N: usize> Default for TinyStack<T, N> {
    fn default() -> Self {
        Self::new()
    }
}

impl<T, const N: usize> Drop for TinyStack<T, N> {
    fn drop(&mut self) {
        while self.pop().is_some() {}
    }
}

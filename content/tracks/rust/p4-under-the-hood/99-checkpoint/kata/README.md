# Checkpoint 4: FixedVec

Build `FixedVec<T, N>`, a vector with room for exactly `N` values stored **inline**: no heap, no
`Vec` inside. It is how crates such as `arrayvec` and `heapless` work, and it needs all three
lessons of the phase: unsafe code over `MaybeUninit`, the traits that make it feel like a
built-in collection, and a macro to build it.

The struct is given:

```rust
pub struct FixedVec<T, const N: usize> {
    slots: [MaybeUninit<T>; N],
    len: usize,            // slots[..len] are initialised, slots[len..] are not
}
```

Keep that invariant true after every method, and write a `// SAFETY:` comment on every unsafe
block saying why it holds.

## Part 1: the unsafe core

- `new()` (a `const fn`), `capacity()` (= `N`) and `is_full()`. `len()` and `is_empty()` come
  from the slice, through `Deref`.
- `push(value) -> Result<(), T>`: when full, give the value **back** in `Err`, untouched.
- `pop() -> Option<T>`: moves the last value out.
- `insert(index, value) -> Result<(), T>`: shifts the values from `index` one place right.
  **Panics** if `index > len`; when full, gives the value back.
- `remove(index) -> T`: shifts the values after `index` one place left. **Panics** if
  `index >= len`.
- `clear()`, `as_slice()`, `as_mut_slice()`.
- `Drop`: `MaybeUninit` never drops its contents, so `FixedVec` must drop its live values,
  **each exactly once**. The tests count drops with a type that records its own.

## Part 2: the traits

- `Deref<Target = [T]>` and `DerefMut`, so `v[0]`, `v.sort()`, `v.iter()` and `v.len()` work.
- `Debug`: like a list, `[1, 2, 3]` (only the live values).
- `PartialEq` between two `FixedVec` with the same values, **whatever their capacities**.
- `Clone` when `T: Clone`: a deep copy. If one `clone` panics halfway, the values already cloned
  must be dropped, and nothing dropped twice.
- `IntoIterator for &FixedVec` (so `for x in &v` works) and an **owning** `IntoIterator` with
  its own `IntoIter<T, N>`: yields the values in order, has an exact `size_hint` (it implements
  `ExactSizeIterator`), and when dropped early drops the values it never yielded.

## Part 3: the macro

`fixed_vec!`, exported from the crate, in three forms, with `N` taken from the type annotation:

```rust
let a: FixedVec<i32, 4> = fixed_vec![];
let b: FixedVec<i32, 4> = fixed_vec![1, 2, 3];          // a trailing comma is fine
let c: FixedVec<String, 4> = fixed_vec![String::from("ab"); 3];
```

- Each argument is evaluated **once** (`value` in the repeat form is cloned).
- More values than capacity **panic** with a message that contains `capacity`.
- It must work where `FixedVec` is not imported: refer to it as `$crate::FixedVec`.

## Part 4: one C function

```rust
#[unsafe(no_mangle)]
pub unsafe extern "C" fn fixed_sum(data: *const i64, len: usize) -> i64
```

Sums `len` numbers at `data`. A null pointer sums to 0. The sum **wraps** on overflow: a panic
cannot unwind out of an `extern "C"` function, so it would abort the whole process (its tests run
in their own binary for that reason).

> This kata reviews the whole phase: associated types, `Deref`, `Index` through a slice,
> `IntoIterator` for references and values, `macro_rules!` with several rules and repetitions,
> hygiene and `$crate`, `MaybeUninit`, `ptr::copy`, drop order, panic safety and `extern "C"`.

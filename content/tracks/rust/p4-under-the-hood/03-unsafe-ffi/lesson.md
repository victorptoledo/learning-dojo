# Unsafe Rust and FFI

> The borrow checker proves your program memory-safe, but some correct programs are beyond what
> it can prove: a `Vec` managing raw memory, a call into a C library, a lock-free queue. `unsafe`
> does not switch the checks off. It lets you do five extra things and makes **you** responsible
> for the rules the compiler can no longer check.

**In this lesson you will learn**

- the five things `unsafe` allows, and what it does not change
- raw pointers, and why creating one is safe but reading through one is not
- writing `unsafe fn` with a `# Safety` contract and `// SAFETY:` comments
- wrapping unsafe code in a safe API, with `MaybeUninit` as the main example
- what undefined behaviour is, and how Miri catches it
- calling C from Rust and Rust from C (`extern "C"`, `CStr`, `CString`)

---

## 1. The five superpowers

Inside an `unsafe` block or `unsafe fn` you may:

1. dereference a raw pointer (`*const T`, `*mut T`);
2. call an `unsafe` function (including foreign functions);
3. access or modify a `static mut`;
4. implement an `unsafe` trait (`Send`, `Sync`);
5. access the fields of a `union`.

Everything else is unchanged: the borrow checker, the type checker and bounds checks on slices
still run inside `unsafe`.

## 2. Raw pointers

```rust
let mut value = 10;
let read = &raw const value;    // *const i32: making a raw pointer is safe
let write = &raw mut value;     // *mut i32

unsafe {
    *write += 1;                // reading or writing through it is not
    println!("{}", *read);      // 11
}
```

A raw pointer may be null, dangling or unaligned, and the compiler tracks no lifetime for it.
Before you dereference it, **you** must know it points at a live, aligned, initialised value,
and that no `&mut` to the same value is alive.

## 3. Contracts: `unsafe fn` and `SAFETY` comments

An `unsafe fn` has preconditions the caller must uphold. Write them in a `# Safety` section; at
every `unsafe` block, write a `// SAFETY:` comment saying why they hold.

```rust
/// Returns the element at `index` without a bounds check.
///
/// # Safety
/// `index` must be less than `slice.len()`.
pub unsafe fn get_fast(slice: &[u32], index: usize) -> u32 {
    // SAFETY: the caller promises index < len.
    unsafe { *slice.as_ptr().add(index) }
}

let values = [1, 2, 3];
// SAFETY: 2 < 3.
let last = unsafe { get_fast(&values, 2) };
```

> [!NOTE]
> Since the 2024 edition, the body of an `unsafe fn` is **not** an unsafe block: each unsafe
> operation needs its own `unsafe { }` (the `unsafe_op_in_unsafe_fn` lint). That keeps every
> dangerous line visible and commented.

## 4. Safe abstractions

The goal is almost never "unsafe code everywhere", but a small unsafe core behind a safe API that
**cannot** be misused. The standard library's `split_at_mut` is the classic: two `&mut` halves of
one slice, which the borrow checker cannot prove disjoint.

```rust
pub fn split_at_mut(slice: &mut [i32], mid: usize) -> (&mut [i32], &mut [i32]) {
    assert!(mid <= slice.len());              // the check that makes the unsafe part sound
    let len = slice.len();
    let ptr = slice.as_mut_ptr();
    // SAFETY: mid <= len, so both ranges are inside the slice and do not overlap.
    unsafe {
        (
            std::slice::from_raw_parts_mut(ptr, mid),
            std::slice::from_raw_parts_mut(ptr.add(mid), len - mid),
        )
    }
}
```

A safe function must be sound for **every** input. Remove the `assert!` and a caller could write
out of bounds without typing `unsafe`: that is a bug in *this* function, not in the caller.

### `MaybeUninit`: memory that is not a value yet

Containers often reserve room before they have values to put in it. `MaybeUninit<T>` is a slot
that may hold a `T` or garbage; the type system no longer assumes it is valid.

```rust
use std::mem::MaybeUninit;

pub struct Pair<T> {
    slots: [MaybeUninit<T>; 2],
    len: usize,                    // the invariant: slots[..len] are initialised
}

impl<T> Pair<T> {
    pub fn new() -> Self {
        Pair { slots: [const { MaybeUninit::uninit() }; 2], len: 0 }
    }

    pub fn push(&mut self, value: T) -> Result<(), T> {
        if self.len == 2 {
            return Err(value);                         // give the value back, do not drop it
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
        // SAFETY: the slot was below len, so it is initialised; len no longer covers it,
        // so it will not be read or dropped again.
        Some(unsafe { self.slots[self.len].assume_init_read() })
    }
}

impl<T> Drop for Pair<T> {
    fn drop(&mut self) {
        while self.pop().is_some() {}                  // drop every live value, exactly once
    }
}
```

`MaybeUninit` never drops its contents, so the `Drop` impl is **required**, or every value leaks.
The whole design rests on one invariant, written next to `len`, and every unsafe line is
justified by it.

## 5. Undefined behaviour and Miri

Breaking an unsafe contract is **undefined behaviour** (UB): the compiler optimises assuming it
never happens, so the program may crash, work by luck, or misbehave far away. The usual ones:

| UB | Example |
| --- | --- |
| reading uninitialised memory | `assume_init` on a slot never written |
| use after free, double free | reading a value twice with `assume_init_read` and dropping both |
| a dangling or misaligned reference | `&*ptr` after the value moved |
| two `&mut` to the same data | aliasing through raw pointers |
| a data race | an `unsafe impl Sync` that is not true |
| an invalid value | a `bool` that is 3, an out-of-range enum |

**Miri** runs your tests in an interpreter that checks every memory access:

```bash
rustup +nightly component add miri
cargo +nightly miri test
```

Run it on any crate with unsafe code. A drop counter in your tests (a type that counts how often
it is dropped) catches leaks and double drops on stable Rust too.

## 6. FFI: talking to C

Rust can call C functions through an `extern "C"` block. In the 2024 edition the block itself is
marked `unsafe`, because you are vouching for the signatures:

```rust
use std::ffi::{CStr, CString, c_char};

unsafe extern "C" {
    fn strlen(text: *const c_char) -> usize;     // from the C standard library
}

let text = CString::new("dojo").unwrap();        // adds the trailing NUL; fails on an inner NUL
// SAFETY: text is a valid NUL-terminated string that lives across the call.
let n = unsafe { strlen(text.as_ptr()) };        // 4
```

And C can call Rust:

```rust
#[unsafe(no_mangle)]                             // keep the symbol name as written
pub extern "C" fn dojo_add(a: i32, b: i32) -> i32 {
    a + b
}
```

| Rust | C | Note |
| --- | --- | --- |
| `i32`, `u64`, `f64` | `int32_t`, `uint64_t`, `double` | |
| `*const c_char` | `const char *` | borrow with `CStr::from_ptr` |
| `CString` | `char *` you own | `into_raw` / `from_raw` to hand it over and take it back |
| `#[repr(C)] struct` | `struct` | Rust's default layout is not C's |
| `Option<extern "C" fn()>` | nullable function pointer | |

Rules that keep FFI sound: check pointers from C for null, never let a panic unwind into C (catch
it or use `extern "C-unwind"`), and free memory with the allocator that created it.

## Practice

1. Write `swap_first_last(slice: &mut [i32])` with raw pointers, then with `slice.swap`. Which do
   you ship?
2. Extend `Pair` to `Trio` with a `const N: usize` parameter and a `len()` method.
3. Write a `Noisy` type that increments a counter in `drop`, push three into a `Pair`-like
   container, drop it, and check the counter.
4. Call `strlen` on a `CString` and compare with `.as_bytes().len()`.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| a raw pointer | `&raw const x`, `&raw mut x`, `slice.as_ptr()` |
| offset a pointer | `ptr.add(n)` (in bounds only) |
| document a contract | `/// # Safety` on the `unsafe fn`, `// SAFETY:` at the call |
| a slot that may be empty | `MaybeUninit<T>`, `.write(v)`, `.assume_init_read()` |
| an array of empty slots | `[const { MaybeUninit::uninit() }; N]` |
| check for UB | `cargo +nightly miri test` |
| call C | `unsafe extern "C" { fn name(...); }` |
| be called from C | `#[unsafe(no_mangle)] pub extern "C" fn name(...)` |
| pass strings to C | `CString::new(s)?.as_ptr()`; read with `CStr::from_ptr(p)` |

**Ready?** Press **Q** in the dojo for the quiz.

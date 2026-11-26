# Checkpoint 4: Traits, Macros and Unsafe

This checkpoint closes the **Traits, Macros and Unsafe** phase. You need both parts:

1. **The quiz** (press **Q**): eight questions over the phase. You need 7 right.
2. **The integration kata** (press **Enter**): `FixedVec<T, N>`, a vector stored inline over
   `MaybeUninit`, with a safe API, the traits of a standard collection, a `fixed_vec!` macro and
   one function callable from C.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Advanced Traits | Why does `IntoIterator` have two associated types? What does `Deref<Target = [T]>` give my type for free? |
| Declarative Macros | How do I accept a trailing comma? Why must an exported macro say `$crate::`? How do I evaluate an argument only once? |
| Unsafe Rust and FFI | Which slots are initialised after each step? Who drops what `MaybeUninit` holds? What happens when a panic reaches an `extern "C"` boundary? |

> [!TIP]
> Write the invariant down before any unsafe line: *slots below `len` are initialised, the others
> are not*. Then check each method against it, including the moment a panic could happen. The
> tests count drops, so a leak or a double drop shows up as a wrong number, not as a crash you
> might never see.

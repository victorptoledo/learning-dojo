# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a configuration library. `Config::parse`
   reports the first bad line with `std::expected`, `get<T>` reads a value as an `int`,
   `double`, `bool` or `std::string` (a concept limits `T`, `if constexpr` picks the code),
   and a move-only `Transaction` changes the config and undoes everything when it goes out of
   scope without `commit()`: RAII for something that is not memory.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| RAII and Smart Pointers | Who owns this resource, and which destructor releases it? `unique_ptr` or `shared_ptr`? |
| Move Semantics | What state is a moved-from object in? Why must a move constructor be `noexcept`? |
| Errors: Exceptions and std::expected | Which guarantee (basic, strong, nothrow) does this function give? Exception or `std::expected`? |
| Templates and Concepts | What does a concept check, and where does the error appear when it fails? What does `if constexpr` discard? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.

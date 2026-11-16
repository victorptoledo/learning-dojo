# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a stock inventory. A class with private data
   and invariants, a `std::map` keyed by sku with heterogeneous lookup, `std::optional` for
   "maybe nothing", money in integer cents parsed from text without overflow, and a small
   command language whose failures are reported by line number.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| The Toolchain and the Build Model | What do the preprocessor, the compiler and the linker each do? What is the one definition rule? |
| Types, Values and References | When is a copy made? What do `const`, `&` and `std::string_view` promise? Which integer mistakes are undefined behaviour? |
| Functions and Control Flow | How do you choose between passing by value, by `const&` and by `&`? What does `std::optional` say that a sentinel does not? |
| Containers, Iterators and Algorithms | When does an iterator become invalid? `std::map` or `std::unordered_map`? What does `operator[]` do on a missing key? |
| Classes and Invariants | What belongs in a constructor? Which members are `const`? What is the rule of zero? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.

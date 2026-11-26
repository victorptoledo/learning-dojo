# Checkpoint 5: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions, most on performance, compile-time programming
   and production C++, a few from earlier phases. You need 7 right.
2. **The integration project** (press **Enter**): a metrics library of the kind every service
   links in. Metric names and bucket bounds can be checked by the **compiler**; a histogram
   records on contiguous memory and **never allocates**; the registry finds a metric by
   `std::string_view` without building a `std::string`; and a **C interface** lets Python, Go
   or Rust call it, with every exception, even `std::bad_alloc`, turned into a status code.
   The tests count every allocation the program makes, and can make one fail on purpose.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Performance: Memory Layout, Allocation, Measuring | Where does this code allocate? Is the data contiguous? What does a lookup cost? |
| Compile-Time Programming | Can the compiler compute or check this? What happens to a `throw` in a constant expression? |
| LRU Cache (kata) | Which container answers which question in constant (or logarithmic) time? |
| C++ in Production: Builds, Safety and Interop | What may cross an `extern "C"` boundary? Who owns the handle, and who frees it? |
| Errors: Exceptions and std::expected (P2) | Which errors are expected, which are exceptional, and what does each become at the boundary? |
| Formatting and Parsing Text (P4) | How do I write into a caller's buffer without overflowing it? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.

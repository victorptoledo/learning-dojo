# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a log pipeline that parses from an
   `io.Reader`, filters with types that satisfy a `Matcher` interface (including a function
   type), selects lazily with `iter.Seq`, counts with a generic function, writes to an
   `io.Writer` and reports bad lines with joined, wrapped errors.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Implicit Interfaces | How does a type satisfy an interface? Why accept `io.Reader`? What is the typed nil trap? |
| Generics and Iterators | Which constraint allows `<`? What does `~` add? What must an iterator do when yield returns false? |
| Generic Collections | Why does the zero value of a generic set work? How does `Take` avoid pulling extra values? |
| Testing | When `Errorf` and when `Fatalf`? What does a fuzz test check, and where are its findings kept? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.

# Record Validator

Bean Validation (`@NotBlank`, `@Size`, `@Valid`) checks objects by reading annotations at run
time. Write a small one for records, with reflection only.

The annotations are already written: `@NotBlank`, `@MaxLength(n)`, `@Range(min, max)` and
`@Valid`. A teammate reports that `@Range` "is ignored": find out why (the lesson says) and fix
the annotation.

`Validator.validate(record)` returns the broken rules as an unmodifiable
`List<Violation>(path, message)`:

| Annotation | On | Violation when | Message |
| --- | --- | --- | --- |
| `@NotBlank` | `String` | null, empty or only spaces | `must not be blank` |
| `@MaxLength(n)` | `String` | longer than `n` (null is fine) | `must be at most n characters` |
| `@Range(min, max)` | `int`, `long`, `Integer`, `Long` | outside `min..max`, limits included (null is fine) | `must be between min and max` |
| `@Valid` | a record, or a `List` of records | (validates what is inside; null is skipped) | |

- Violations come in **component declaration order**; on one component, `@NotBlank` comes
  before `@MaxLength`.
- Paths: the component name (`city`), with the parent in front for `@Valid`:
  `address.city`, `lines[1].sku`.
- An annotation on the wrong type (`@Range` on a `String`, `@NotBlank` on an `int`, `@Valid` on
  a `String`) is a programming error: throw `IllegalStateException` naming
  `Record.component` (for example `Order.name`), **even when the value is null**. Check the
  annotations when you first meet a class, not when a value happens to be set.
- An accessor that throws must come out as **its own** exception, not as a reflection wrapper.
- `validate(null)` and an object that is not a record: `IllegalArgumentException`.

> [!TIP]
> `getClass().isRecord()`, `getRecordComponents()`, `component.getAnnotation(...)`,
> `component.getType()`, `component.getAccessor().invoke(record)` and
> `InvocationTargetException.getCause()`. Look a class's components up once and keep them
> (a `ClassValue` or a `ConcurrentHashMap` per class).

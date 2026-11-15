---
from: python
status: draft
concept: "Type-level computation"
from_way: "Typing has a few special forms (Required, NotRequired, ReadOnly for TypedDict, ParamSpec), but no way to compute new TypedDicts from old ones."
native_way: "Full type-level computation: Partial, Pick, Omit, mapped types over keys, conditional types with infer, template literal types."
sensei_tip: "This is the part of TypeScript Python typing has no answer to. Do not overdo it: one derived type with a clear name beats a clever five-line conditional."
---

| Python | TypeScript |
| --- | --- |
| `class UserPatch(TypedDict, total=False)` written by hand | `Partial<User>` |
| A second TypedDict without a key | `Omit<User, "password">` |
| `ParamSpec`, `Concatenate` | `Parameters<F>`, tuples with `infer` |
| `Literal["onOpen", "onClose"]` | `` `on${Capitalize<Event>}` `` |
| `typing.assert_type(x, int)` | `Expect<Equal<typeof x, number>>` |

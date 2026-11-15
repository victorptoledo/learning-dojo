---
from: java
status: draft
concept: "Types computed from other types"
from_way: "Types are declared one by one; to get a 'User without password' you write a new class or record, or use code generation (MapStruct, Lombok)."
native_way: "Types are computed: Omit<User, \"password\">, Partial<User>, mapped and conditional types derive new shapes at compile time."
sensei_tip: "Many Java DTO classes become one-line derived types. Keep a single source type and derive the request, response and patch types from it."
---

| Java | TypeScript |
| --- | --- |
| Separate `UserDto` record | `type PublicUser = Omit<User, "password">` |
| Builder or nullable fields for patches | `Partial<User>` |
| `EnumMap<Status, Integer>` | `Record<Status, number>` |
| Annotation processors / code generation | Mapped and conditional types |
| No equivalent | `ReturnType<typeof f>`, `Awaited<...>` |

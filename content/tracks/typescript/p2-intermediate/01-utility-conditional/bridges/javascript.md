---
from: javascript
status: draft
concept: "Describing data transformations in types"
from_way: "Functions such as pick(obj, keys), omit and mapValues change the shape of objects, and the result shape is in the developer's head."
native_way: "Pick, Omit, mapped and conditional types describe the result shape, so the compiler knows what pick(user, [\"id\"]) returns."
sensei_tip: "Whenever a JavaScript helper reshapes objects, there is a type operator that describes it. Read library typings with this lesson in mind and they stop looking like magic."
---

| JavaScript | TypeScript |
| --- | --- |
| `const { password, ...publicUser } = user` | `Omit<User, "password">` |
| `Object.fromEntries(keys.map(k => [k, true]))` | `{ [K in Keys]: boolean }` |
| `` `get${name[0].toUpperCase()}${name.slice(1)}` `` | `` `get${Capitalize<Name>}` `` |
| `await fn()` | `Awaited<ReturnType<typeof fn>>` |

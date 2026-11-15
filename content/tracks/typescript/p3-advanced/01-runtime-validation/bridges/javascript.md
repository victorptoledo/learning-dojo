---
from: javascript
status: draft
concept: "Checking input you used to trust"
from_way: "Input is used as it arrives, maybe with manual checks or a validator such as joi or ajv; the shape lives in documentation."
native_way: "Input is unknown until a schema parses it; the schema gives both the runtime check and the static type."
sensei_tip: "TypeScript does not make JSON safer by itself; the cast as User is the same trust as plain JavaScript. The schema is what changes things."
---

| JavaScript | TypeScript |
| --- | --- |
| `const user = await res.json()` | `const user = User.parse(await res.json())` |
| `if (!body.email) return 400` | `const r = Body.safeParse(body); if (!r.success) ...` |
| joi / ajv schemas + JSDoc types | zod schema + `z.infer` |
| `Number(process.env.PORT) \|\| 3000` | A config schema validated at start-up |

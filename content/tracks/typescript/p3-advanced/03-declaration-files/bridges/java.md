---
from: java
status: draft
concept: "Types for code you do not compile"
from_way: "Libraries ship compiled classes with their types in the bytecode; there is no separate type description to maintain."
native_way: "JavaScript libraries carry no types, so types come in .d.ts files: shipped by the package, from @types/* on DefinitelyTyped, or written by you."
sensei_tip: "A .d.ts is roughly a Java interface file for code written in another language. When a library's types are wrong, you can fix or extend them locally, which Java never lets you do."
---

| Java | TypeScript |
| --- | --- |
| Types inside the `.jar` | `.d.ts` in the package or in `@types/<name>` |
| Maven dependency | `npm install x` plus maybe `npm install -D @types/x` |
| Adding a field to a library class | Module augmentation of its interface |
| `javadoc` on the API | JSDoc comments in the `.d.ts`, shown by the editor |
| Publishing a `-sources.jar` | `declarationMap` |

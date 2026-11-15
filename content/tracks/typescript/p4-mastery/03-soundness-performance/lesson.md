# Soundness, Variance and Type-Checking Performance

A type system is **sound** when a program that type-checks can never hold a value of the wrong
type at runtime. TypeScript is deliberately **not** sound: its design goals put "describe the
JavaScript people really write" ahead of a proof. Senior TypeScript work means knowing exactly
where the holes are, closing the ones that matter with compiler options and patterns, and
keeping the checker fast as types grow.

## The known holes

| Hole | Example | Mitigation |
| --- | --- | --- |
| `any` | `JSON.parse`, `response.json()`, old typings | Assign to `unknown`; lint `no-unsafe-*`; validate at boundaries |
| Type assertions | `value as User`, `value!` | Use them only after a check the compiler cannot see; prefer type predicates |
| Index access | `array[10]`, `record["missing"]` | `noUncheckedIndexedAccess` |
| Optional vs `undefined` | `{ name?: string }` accepts `{ name: undefined }` | `exactOptionalPropertyTypes` |
| Method bivariance | Methods compare parameters both ways | Declare callbacks as properties: `onChange: (v: T) => void` |
| Array covariance | `const animals: Animal[] = dogs; animals.push(cat)` | `readonly` arrays when you only read |
| Mutation after narrowing | A narrowed property changed by a function call | Copy to a `const` before narrowing |
| Wrong declaration files | A `.d.ts` that lies about a library | Report or fix upstream; augment or wrap locally |

### Method bivariance in detail

```ts
interface MethodHandler<T> {
  handle(value: T): void;          // method syntax: parameters checked bivariantly
}
interface PropertyHandler<T> {
  handle: (value: T) => void;      // property syntax: checked contravariantly with strictFunctionTypes
}

declare const dogMethod: MethodHandler<Dog>;
declare const dogProperty: PropertyHandler<Dog>;
const a: MethodHandler<Animal> = dogMethod;      // compiles: a cat can now reach a dog-only handler
const b: PropertyHandler<Animal> = dogProperty;  // error: Animal is not assignable to Dog
```

`strictFunctionTypes` (part of `strict`) checks function-typed **properties** correctly, but
method declarations keep the old bivariant rule so that common patterns like `Array<Dog>` being
an `Array<Animal>` keep working. When you design an interface whose callbacks must be checked
strictly, write them as properties.

### exactOptionalPropertyTypes

```ts
interface Settings { theme?: "dark" | "light" }
const s: Settings = { theme: undefined };   // allowed by default; an error with exactOptionalPropertyTypes
```

With the option, "may be absent" and "may be `undefined`" are different, which matches what
`"theme" in s` and `Object.keys` see at runtime. It is not part of `strict` because it breaks a
lot of existing code; turn it on in new projects.

## Variance

**Variance** says how a generic type's assignability follows its type argument:

| Variance | Meaning | Example |
| --- | --- | --- |
| Covariant | `Box<Dog>` is assignable to `Box<Animal>` | Read-only things: `ReadonlyArray<T>`, return types |
| Contravariant | `Box<Animal>` is assignable to `Box<Dog>` | Things that only consume `T`: callback parameters |
| Invariant | Neither | Read and write: a mutable cell |
| Bivariant | Both (unsound) | Method parameters |

TypeScript **infers** variance from the structure of a type. For complex types you can state it
with **variance annotations** (TypeScript 4.7):

```ts
interface Producer<out T> { get(): T }
interface Consumer<in T> { accept(value: T): void }
interface Cell<in out T> { get(): T; set(value: T): void }
```

Annotations document intent, make the compiler check that the type really has that variance,
and can speed up checks of deeply generic types, because the compiler no longer has to measure
the variance itself.

## Keeping the checker fast

Large codebases spend most compile time in a few expensive types. TypeScript 7's native
compiler is about ten times faster, but a pathological type is pathological at any speed.

### Measure first

```bash
tsc --extendedDiagnostics       # time per phase, memory, number of types and instantiations
tsc --generateTrace trace/      # a trace to open in Perfetto or chrome://tracing
```

Look at "Instantiations" and "Check time". A jump after a change points at the guilty type.

### Patterns that help

- **Prefer `interface extends` to large intersections**: interfaces are cached by name;
  intersections of many object types are re-computed and flattened again and again.
- **Name complex types** and annotate function **return types** in exported APIs: the compiler
  (and `isolatedDeclarations`) can reuse a declared type instead of inferring a huge one.
- **Avoid huge unions** (thousands of members) in hot paths: assignability to a union is checked
  member by member. Template literal types that multiply unions (`${A}-${B}-${C}`) explode
  quickly.
- **Limit recursion**: deep recursive conditional types hit "Type instantiation is excessively
  deep and possibly infinite"; keep recursion tail-shaped and bounded.
- **Split the program** with project references so each project is checked against declaration
  files, not sources.
- **Keep `skipLibCheck: true`** and avoid including unnecessary files (`include`, `exclude`,
  `types`).

### Editor performance

The language service re-checks as you type. If the editor is slow in one file, that file usually
contains or uses one of the expensive types above. TypeScript 7's language server is native and
much faster, which hides problems until the codebase grows; measure anyway.

## Designing types that age well

- Make **invalid states unrepresentable** (discriminated unions), and constructors the only way
  to create checked values (brands).
- Keep public types **simple and named**; keep clever type-level code internal and tested.
- Accept wide types and return narrow ones: parameters as `readonly T[]` or `Iterable<T>`,
  results as concrete types.
- Treat any `as`, `!`, `any` or `@ts-expect-error` in production code as a debt with a comment
  saying why it is safe.

## Summary

- TypeScript is intentionally unsound: know the holes (`any`, assertions, indexing, optional
  properties, method bivariance, array covariance) and the options that close them
  (`noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, property-style callbacks, `readonly`).
- Variance is inferred; annotate with `in` and `out` to document, check and speed it up.
- Measure with `--extendedDiagnostics` and `--generateTrace`; prefer interfaces, named types,
  explicit return types, bounded recursion and project references.

## Further reading

- TypeScript Handbook, "Type Compatibility" (a note on soundness): <https://www.typescriptlang.org/docs/handbook/type-compatibility.html>
- TypeScript design goals (and non-goals): <https://github.com/microsoft/TypeScript/wiki/TypeScript-Design-Goals>
- TypeScript wiki, "Performance": <https://github.com/microsoft/TypeScript/wiki/Performance>
- TypeScript 4.7 release notes, variance annotations: <https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-7.html>
- TSConfig reference, `exactOptionalPropertyTypes`: <https://www.typescriptlang.org/tsconfig/#exactOptionalPropertyTypes>
- TSConfig reference, `strictFunctionTypes`: <https://www.typescriptlang.org/tsconfig/#strictFunctionTypes>
- roadmap.sh, TypeScript topic "Advanced Types": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/advanced-types@2F7vOL__v9dLBohA263aj.md>

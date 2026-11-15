# Types, Inference and Structural Typing

This lesson covers the everyday types and the one idea that makes TypeScript different from
Java or C#: **a type is a shape, not a name.** If a value has the properties a type asks for,
it *is* of that type, whoever created it.

## Primitive types and annotations

```ts
let title: string = "Dojo";
let count: number = 3;        // one number type: 64-bit floats (and integers up to 2^53)
let big: bigint = 10n ** 30n; // arbitrary-size integers
let done: boolean = false;
let nothing: null = null;
let missing: undefined = undefined;
const id: symbol = Symbol("id");
```

You rarely write these annotations, because TypeScript **infers** types from values:

```ts
let title = "Dojo";      // string
const kind = "kata";     // "kata": a const can never change, so its type is the literal
let scores = [90, 72];   // number[]
title = 42;              // error TS2322: Type 'number' is not assignable to type 'string'
```

The rule of thumb: annotate **function parameters** and **exported** functions' return types,
and let inference do the rest. Parameters cannot be inferred from the body.

## Arrays, tuples and objects

```ts
const tags: string[] = ["ts", "node"];            // also Array<string>
const frozen: readonly string[] = tags;           // no push, no index assignment
const point: [x: number, y: number] = [3, 4];     // a tuple: fixed length and position types
const user: { name: string; email?: string; readonly id: number } = { name: "Ada", id: 1 };
```

- `email?` is **optional**: its type is `string | undefined` and the key may be absent.
- `readonly id` cannot be reassigned through this type (it is a compile-time promise only).
- With `noUncheckedIndexedAccess` (used in the dojo), `tags[5]` has type `string | undefined`,
  because an index may be out of range. Narrow it before use.

Objects used as dictionaries take an **index signature** or `Record`:

```ts
const stock: Record<string, number> = { apples: 3 };
const prices: { [sku: string]: number } = {};
```

## Structural typing

```ts
interface Named {
  name: string;
}

class Dog {
  constructor(name: string) { this.name = name; }
  name: string;
  bark() { return "woof"; }
}

function hello(who: Named) {
  return `hello ${who.name}`;
}

hello(new Dog("Rex"));         // OK: a Dog has a name
const robot = { name: "R2", serial: 42 };
hello(robot);                  // OK: robot has at least a name
```

`Dog` never says `implements Named`, and it still is one. Compatibility is decided by
**structure**: a type `A` is assignable to `B` when `A` has every property `B` requires, with
compatible types. Extra properties are fine. This is called **structural typing** (or "duck
typing checked at compile time"); Java, C# and Rust use **nominal** typing, where the declared
name and the `implements` list decide.

Why? JavaScript code passes plain objects around all the time. A nominal system would force
you to wrap every object literal in a class. Structural typing describes JavaScript as it is.

### Excess property checks

There is one exception that surprises everyone:

```ts
hello({ name: "Ada", age: 36 });   // error TS2353: Object literal may only specify known properties
```

When you pass a **fresh object literal** directly, TypeScript reports properties the target
type does not know, because they are almost always typos (`{ nmae: "Ada" }`). Assign the
literal to a variable first, or widen the target type, and the check no longer applies.

### Where structural typing bites

Two types with the same shape are interchangeable, even when they mean different things:

```ts
type UserId = string;
type OrderId = string;
function cancel(order: OrderId) {}
const user: UserId = "usr_1";
cancel(user);   // compiles: both are just string
```

A type alias is only a new **name** for the same structure. When you need distinct types,
add a **brand**, a property that only exists in the type system:

```ts
type UserId = string & { readonly __brand: "UserId" };
type OrderId = string & { readonly __brand: "OrderId" };
const toOrderId = (text: string) => text as OrderId; // the one place that may create one
```

Now `cancel(user)` is an error. You will see brands again with zod in phase 3.

## any, unknown and the escape hatches

`any` turns checking **off** for a value and everything it touches:

```ts
const data: any = JSON.parse(text);
data.user.nmae.toUpperCase();    // compiles; crashes at runtime
```

Prefer `unknown` (next lesson), which forces you to check before use. A **type assertion**
`value as User` tells the compiler "trust me"; it changes nothing at runtime and can lie.
The **non-null assertion** `value!` removes `null | undefined` the same way. Use both rarely,
right after a check the compiler cannot see.

## Type compatibility for functions

A function with **fewer** parameters can stand in for one with more, because JavaScript
ignores extra arguments:

```ts
const names = ["a", "b"].map((value) => value.toUpperCase()); // map passes (value, index, array)
```

Return types are compared structurally like everything else.

## Summary

- Let inference type local values; annotate parameters and exported return types.
- Arrays, readonly arrays, tuples, optional and readonly properties, `Record`.
- Compatibility is structural: having the required properties is enough.
- Fresh object literals get excess property checks; aliases are not distinct types, brands are.
- `any` disables checking; prefer `unknown`, and treat `as` and `!` as promises you must keep.

## Further reading

- TypeScript Handbook, "Everyday Types": <https://www.typescriptlang.org/docs/handbook/2/everyday-types.html>
- TypeScript Handbook, "Object Types": <https://www.typescriptlang.org/docs/handbook/2/objects.html>
- TypeScript Handbook, "Type Compatibility": <https://www.typescriptlang.org/docs/handbook/type-compatibility.html>
- TSConfig reference, `noUncheckedIndexedAccess`: <https://www.typescriptlang.org/tsconfig/#noUncheckedIndexedAccess>
- roadmap.sh, TypeScript topic "Type Compatibility": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/type-compatibility@alMJCEyUZ90xz-2_g3YRj.md>

# Interfaces, Type Aliases, Functions and Classes

Two keywords name object types, `interface` and `type`, and people argue about them more than
they deserve. This lesson settles that, then covers how to type functions (parameters,
overloads, callbacks) and how classes work in TypeScript, including what changed now that
Node.js runs `.ts` files by erasing types.

## interface vs type

```ts
interface User {
  id: number;
  name: string;
}

type Point = { x: number; y: number };
```

For a plain object shape, both work and mean the same. The differences:

| | `interface` | `type` alias |
| --- | --- | --- |
| Object shapes | Yes | Yes |
| Unions, tuples, primitives, mapped and conditional types | No | Yes: `type Id = string \| number` |
| Extension | `interface Admin extends User { ... }` | Intersection: `type Admin = User & { ... }` |
| **Declaration merging** | Two `interface User` declarations merge into one | A second `type User` is an error |
| Error messages and performance | Named in messages; `extends` is cached and checks conflicts | Large intersections are re-computed and conflicts become `never` silently |

A practical rule, and the one this track follows: **use `interface` for object shapes that
others may extend, and `type` for everything else** (unions, function types, tuples, derived
types). Be consistent within a codebase; a lint rule can enforce it.

Declaration merging is a feature for libraries: it is how you add a property to `Window` or to
Express's `Request` (phase 3). In application code it mostly causes surprises, which is one
reason some teams prefer `type` everywhere.

`extends` checks that the new members are compatible (`interface B extends A { id: string }`
fails when `A.id` is a `number`), while `A & { id: string }` quietly makes `id: never`.

## Typing functions

```ts
function area(width: number, height = width): number {  // default: type inferred
  return width * height;
}

function join(separator: string, ...parts: string[]): string {   // rest parameter
  return parts.join(separator);
}

function find(users: User[], name?: string): User | undefined {   // optional parameter
  return users.find((user) => user.name === name);
}

const double = (n: number): number => n * 2;
```

A **function type** describes a callback:

```ts
type Comparator<T> = (a: T, b: T) => number;
type Handler = (event: { type: string }) => void;
```

A callback typed to return `void` may still return a value; the caller just promises not to
use it. That is why `array.forEach((x) => list.push(x))` compiles although `push` returns a
number.

Prefer an **options object** over many positional parameters once a function has more than
two or three, especially booleans: `createUser(name, { admin: true, notify: false })` reads
better than `createUser(name, true, false)`.

### Overloads

When the return type depends on the arguments in a way a union cannot express, declare
**overload signatures** above one implementation:

```ts
function parse(input: string): number;
function parse(input: string[]): number[];
function parse(input: string | string[]): number | number[] {
  return Array.isArray(input) ? input.map(Number) : Number(input);
}

const one = parse("1");          // number
const many = parse(["1", "2"]);  // number[]
```

Callers see only the overload signatures; the implementation signature is hidden. Prefer a
union parameter or a generic (next lesson) when they are enough, and use overloads when the
output type really follows the input.

### this

TypeScript can type `this` as a fake first parameter: `function onClick(this: HTMLElement)`.
Arrow functions capture the surrounding `this`, which is why class methods passed as callbacks
are often written as arrow-function properties or wrapped: `button.addEventListener("click",
() => this.save())`.

## Classes

```ts
interface Greeter {
  greet(): string;
}

class Person implements Greeter {
  static count = 0;
  readonly name: string;
  #secret = "hidden";             // a JavaScript private field: really private at runtime
  protected nickname?: string;    // TypeScript-only visibility

  constructor(name: string) {
    this.name = name;
    Person.count += 1;
  }

  greet(): string {
    return `Hi, I am ${this.name}`;
  }

  get initials(): string {
    return this.name.slice(0, 1);
  }
}

class Employee extends Person {
  readonly team: string;

  constructor(name: string, team: string) {
    super(name);
    this.team = team;
  }

  override greet(): string {      // `override` is required with noImplicitOverride
    return `${super.greet()} from ${this.team}`;
  }
}
```

Things to know:

- `implements` only **checks** the class against the interface; thanks to structural typing,
  an object can satisfy `Greeter` without it.
- `private` and `protected` are compile-time only: the fields are ordinary properties at
  runtime. `#secret` is a JavaScript private field, enforced by the engine. Prefer `#` for real
  encapsulation.
- `abstract class` declares members subclasses must implement; it cannot be instantiated.
- **Parameter properties** (`constructor(private readonly db: Database) {}`) declare and assign
  a field in one go. They generate code, so they are **not erasable**: Node.js type stripping
  rejects them and `erasableSyntaxOnly` reports them. Declare the field and assign it, as above.
- Legacy decorators (`experimentalDecorators`) are not erasable either. Standard ECMAScript
  decorators are supported by `tsc` but not by Node's type stripping yet.

### Classes or plain objects?

Classes are good for things with identity and behaviour that hold private state: a connection
pool, a cache, an event emitter. Data that travels (JSON, events, records) is better as plain
object types: they serialise without losing anything, compare structurally and work with
`structuredClone`. A class instance turned into JSON and back is no longer an instance.

## Summary

- `interface` for extendable object shapes, `type` for unions, functions, tuples and derived
  types; know declaration merging and that `extends` catches conflicts `&` hides.
- Type parameters, defaults, rest parameters and callbacks; use options objects; overloads when
  the output follows the input.
- Classes: `implements` checks, `#private` is real, `private` is not; `override`; no parameter
  properties when Node.js strips the types.

## Further reading

- TypeScript Handbook, "More on Functions": <https://www.typescriptlang.org/docs/handbook/2/functions.html>
- TypeScript Handbook, "Classes": <https://www.typescriptlang.org/docs/handbook/2/classes.html>
- TypeScript Handbook, "Everyday Types", differences between type aliases and interfaces: <https://www.typescriptlang.org/docs/handbook/2/everyday-types.html#differences-between-type-aliases-and-interfaces>
- MDN, "Private elements": <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Classes/Private_elements>
- TSConfig reference, `erasableSyntaxOnly`: <https://www.typescriptlang.org/tsconfig/#erasableSyntaxOnly>
- roadmap.sh, TypeScript topic "Types vs Interfaces": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/types-vs-interfaces@TxkEpoOiuUm-fXJuFVKcH.md>

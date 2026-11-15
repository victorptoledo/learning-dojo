# Shapes

`shapes.ts` defines `Shape` as a **discriminated union**: every member has a literal `kind`
field, and checking it narrows the shape to one member. Write five functions.

| Function | Returns |
| --- | --- |
| `area(shape)` | The area. A circle is `π·r²`, a rectangle `width·height`, a triangle uses Heron's formula with `s = (a + b + c) / 2` |
| `perimeter(shape)` | The perimeter (`2·π·r` for a circle) |
| `describe(shape)` | `"circle r=2"`, `"rectangle 3x4"` or `"triangle 3-4-5"` |
| `parseShape(input: unknown)` | A `Shape` built from untrusted data, or `null` when it is not valid (see below) |
| `largest(shapes)` | The shape with the largest area, the first one on a tie, `undefined` for an empty list |

`parseShape` receives `unknown`, for example the result of `JSON.parse`. It returns a shape
only when the input is a plain object (not `null`, not an array) with a known `kind` and the
fields of that kind, each a **finite number greater than 0**. A triangle must also be
possible: each side shorter than the sum of the other two. The result keeps only the fields
of the shape, so `{ kind: "rectangle", width: 1, height: 2, color: "red" }` becomes
`{ kind: "rectangle", width: 1, height: 2 }`.

Use a `switch` on `shape.kind` and end it with `default: return assertNever(shape);`. If you
later add a member to `Shape`, every switch that forgot it stops compiling.

Do not use `any` or `as` casts: narrow `unknown` with `typeof`, `in`, `Array.isArray` and type
predicates (`function isRecord(value: unknown): value is Record<string, unknown>`).

Run the tests with `npm test` after `npm install`. There are 10 tests: 1 in
`tests/visible.test.ts` and 9 hidden ones.

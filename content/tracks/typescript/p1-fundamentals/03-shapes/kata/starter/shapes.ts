// Shapes as a discriminated union: the "kind" field tells TypeScript which shape it holds.

export type Circle = { kind: "circle"; radius: number };
export type Rectangle = { kind: "rectangle"; width: number; height: number };
export type Triangle = { kind: "triangle"; a: number; b: number; c: number };

export type Shape = Circle | Rectangle | Triangle;

/** Call it in the default branch of a switch: it only compiles when every case is handled. */
export function assertNever(value: never): never {
  throw new Error(`unexpected value: ${JSON.stringify(value)}`);
}

/** The area of the shape (Heron's formula for a triangle). */
export function area(shape: Shape): number {
  throw new Error("todo: write area()");
}

/** The perimeter of the shape. */
export function perimeter(shape: Shape): number {
  throw new Error("todo: write perimeter()");
}

/** "circle r=2", "rectangle 3x4" or "triangle 3-4-5". */
export function describe(shape: Shape): string {
  throw new Error("todo: write describe()");
}

/**
 * Turns untrusted data (for example from JSON.parse) into a Shape, or returns null when it is
 * not a valid one: an object with a known kind and the right fields, all positive finite
 * numbers, and for a triangle each side shorter than the sum of the other two.
 */
export function parseShape(input: unknown): Shape | null {
  throw new Error("todo: write parseShape()");
}

/** The shape with the largest area; the first one on a tie; undefined for no shapes. */
export function largest(shapes: readonly Shape[]): Shape | undefined {
  throw new Error("todo: write largest()");
}

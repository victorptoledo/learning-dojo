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
  switch (shape.kind) {
    case "circle":
      return Math.PI * shape.radius ** 2;
    case "rectangle":
      return shape.width * shape.height;
    case "triangle": {
      const s = perimeter(shape) / 2;
      return Math.sqrt(s * (s - shape.a) * (s - shape.b) * (s - shape.c));
    }
    default:
      return assertNever(shape);
  }
}

/** The perimeter of the shape. */
export function perimeter(shape: Shape): number {
  switch (shape.kind) {
    case "circle":
      return 2 * Math.PI * shape.radius;
    case "rectangle":
      return 2 * (shape.width + shape.height);
    case "triangle":
      return shape.a + shape.b + shape.c;
    default:
      return assertNever(shape);
  }
}

/** "circle r=2", "rectangle 3x4" or "triangle 3-4-5". */
export function describe(shape: Shape): string {
  switch (shape.kind) {
    case "circle":
      return `circle r=${shape.radius}`;
    case "rectangle":
      return `rectangle ${shape.width}x${shape.height}`;
    case "triangle":
      return `triangle ${shape.a}-${shape.b}-${shape.c}`;
    default:
      return assertNever(shape);
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isPositive(value: unknown): value is number {
  return typeof value === "number" && Number.isFinite(value) && value > 0;
}

/**
 * Turns untrusted data (for example from JSON.parse) into a Shape, or returns null when it is
 * not a valid one: an object with a known kind and the right fields, all positive finite
 * numbers, and for a triangle each side shorter than the sum of the other two.
 */
export function parseShape(input: unknown): Shape | null {
  if (!isRecord(input)) {
    return null;
  }
  switch (input.kind) {
    case "circle": {
      const { radius } = input;
      return isPositive(radius) ? { kind: "circle", radius } : null;
    }
    case "rectangle": {
      const { width, height } = input;
      return isPositive(width) && isPositive(height) ? { kind: "rectangle", width, height } : null;
    }
    case "triangle": {
      const { a, b, c } = input;
      if (!isPositive(a) || !isPositive(b) || !isPositive(c)) {
        return null;
      }
      const possible = a < b + c && b < a + c && c < a + b;
      return possible ? { kind: "triangle", a, b, c } : null;
    }
    default:
      return null;
  }
}

/** The shape with the largest area; the first one on a tie; undefined for no shapes. */
export function largest(shapes: readonly Shape[]): Shape | undefined {
  let best: Shape | undefined;
  for (const shape of shapes) {
    if (best === undefined || area(shape) > area(best)) {
      best = shape;
    }
  }
  return best;
}

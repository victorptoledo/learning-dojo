import assert from "node:assert/strict";
import { test } from "node:test";
import { area, describe, largest, parseShape, perimeter, type Shape } from "../shapes.ts";

const circle: Shape = { kind: "circle", radius: 2 };
const square: Shape = { kind: "rectangle", width: 3, height: 3 };
const triangle: Shape = { kind: "triangle", a: 3, b: 4, c: 5 };

const close = (actual: number, expected: number) =>
  assert.ok(Math.abs(actual - expected) < 1e-9, `${actual} is not ${expected}`);

test("area of each kind", () => {
  close(area(circle), Math.PI * 4);
  close(area(square), 9);
  close(area(triangle), 6);
});

test("perimeter of each kind", () => {
  close(perimeter(circle), 4 * Math.PI);
  close(perimeter(square), 12);
  close(perimeter(triangle), 12);
});

test("describe each kind", () => {
  assert.equal(describe(circle), "circle r=2");
  assert.equal(describe({ kind: "rectangle", width: 3, height: 4.5 }), "rectangle 3x4.5");
  assert.equal(describe(triangle), "triangle 3-4-5");
});

test("parseShape accepts valid shapes from JSON", () => {
  assert.deepEqual(parseShape(JSON.parse('{"kind":"circle","radius":1.5}')), {
    kind: "circle",
    radius: 1.5,
  });
  assert.deepEqual(parseShape({ kind: "triangle", a: 3, b: 4, c: 5 }), triangle);
});

test("parseShape keeps only the known fields", () => {
  assert.deepEqual(parseShape({ kind: "rectangle", width: 1, height: 2, color: "red" }), {
    kind: "rectangle",
    width: 1,
    height: 2,
  });
});

test("parseShape rejects what is not an object or has an unknown kind", () => {
  for (const input of [null, undefined, 42, "circle", [], { kind: "hexagon", side: 1 }, {}]) {
    assert.equal(parseShape(input), null, JSON.stringify(input));
  }
});

test("parseShape rejects missing, wrong-typed and non-positive fields", () => {
  for (const input of [
    { kind: "circle" },
    { kind: "circle", radius: "2" },
    { kind: "circle", radius: 0 },
    { kind: "circle", radius: -1 },
    { kind: "circle", radius: Number.NaN },
    { kind: "circle", radius: Number.POSITIVE_INFINITY },
    { kind: "rectangle", width: 2 },
  ]) {
    assert.equal(parseShape(input), null, JSON.stringify(input));
  }
});

test("parseShape rejects impossible triangles", () => {
  assert.equal(parseShape({ kind: "triangle", a: 1, b: 2, c: 3 }), null);
  assert.equal(parseShape({ kind: "triangle", a: 1, b: 10, c: 2 }), null);
});

test("largest picks the largest area, the first on a tie", () => {
  const other: Shape = { kind: "rectangle", width: 1, height: 9 };
  assert.equal(largest([square, circle, triangle]), circle);
  assert.equal(largest([square, other]), square);
  assert.equal(largest([]), undefined);
});

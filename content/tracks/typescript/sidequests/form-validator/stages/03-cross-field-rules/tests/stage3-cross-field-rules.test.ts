import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { defineSchema, validate } from "../validator.ts";

const signup = defineSchema({
  password: [{ kind: "required" }, { kind: "minLength", min: 8 }],
  confirm: [{ kind: "required" }, { kind: "sameAs", field: "password" }],
  nickname: [
    { kind: "custom", check: (value, form) => (value === form.password ? "must not be the password" : null) },
  ],
});

describe("sameAs", () => {
  test("passes when both fields hold the same trimmed text", () => {
    const result = validate(signup, { password: "correct horse", confirm: "  correct horse " });
    assert.deepEqual(result, {
      ok: true,
      values: { password: "correct horse", confirm: "correct horse", nickname: "" },
    });
  });

  test("names the other field when they differ", () => {
    const result = validate(signup, { password: "correct horse", confirm: "correct house" });
    assert.deepEqual(result, { ok: false, errors: [{ field: "confirm", messages: ["must match password"] }] });
  });

  test("compares with a missing field as empty text", () => {
    const schema = defineSchema({ a: [], b: [{ kind: "sameAs", field: "a" }] });
    assert.deepEqual(validate(schema, { b: "x" }), {
      ok: false,
      errors: [{ field: "b", messages: ["must match a"] }],
    });
  });

  test("compares with the other field even when it has errors itself", () => {
    const result = validate(signup, { password: "short", confirm: "short" });
    assert.deepEqual(result, {
      ok: false,
      errors: [{ field: "password", messages: ["must be at least 8 characters"] }],
    });
  });

  test("compares exactly, case included", () => {
    const result = validate(signup, { password: "Correct horse", confirm: "correct horse" });
    assert.equal(result.ok, false);
  });
});

describe("custom", () => {
  test("its message is reported like any other", () => {
    const result = validate(signup, { password: "correct horse", confirm: "correct horse", nickname: " correct horse " });
    assert.deepEqual(result, {
      ok: false,
      errors: [{ field: "nickname", messages: ["must not be the password"] }],
    });
  });

  test("returning null follows the rule", () => {
    const result = validate(signup, { password: "correct horse", confirm: "correct horse", nickname: "ana" });
    assert.equal(result.ok, true);
  });

  test("gets the trimmed value and every value of the form, missing ones empty", () => {
    const seen: unknown[] = [];
    const schema = defineSchema({
      start: [],
      end: [],
      note: [
        {
          kind: "custom",
          check: (value, form) => {
            seen.push(value, { ...form });
            return null;
          },
        },
      ],
    });
    validate(schema, { start: " 9 ", note: "  hi  ", extra: "x" });
    assert.deepEqual(seen, ["hi", { start: "9", end: "", note: "hi" }]);
  });

  test("can compare numbers of two fields", () => {
    const booking = defineSchema({
      from: [{ kind: "required" }, { kind: "number" }],
      to: [
        { kind: "required" },
        { kind: "number" },
        { kind: "custom", check: (value, form) => (Number(value) < Number(form.from) ? "must not be before from" : null) },
      ],
    });
    assert.deepEqual(validate(booking, { from: "10", to: "4" }), {
      ok: false,
      errors: [{ field: "to", messages: ["must not be before from"] }],
    });
    assert.equal(validate(booking, { from: "10", to: "12" }).ok, true);
  });

  test("keeps the order of the rules with the other messages", () => {
    const schema = defineSchema({
      code: [
        { kind: "custom", check: () => "first" },
        { kind: "minLength", min: 5 },
        { kind: "custom", check: () => "third" },
      ],
    });
    assert.deepEqual(validate(schema, { code: "abc" }), {
      ok: false,
      errors: [{ field: "code", messages: ["first", "must be at least 5 characters", "third"] }],
    });
  });
});

describe("empty values", () => {
  test("an empty optional field does not run sameAs or custom", () => {
    let calls = 0;
    const schema = defineSchema({
      a: [],
      b: [
        { kind: "sameAs", field: "a" },
        {
          kind: "custom",
          check: () => {
            calls += 1;
            return "never";
          },
        },
      ],
    });
    assert.deepEqual(validate(schema, { a: "x", b: "   " }), { ok: true, values: { a: "x", b: "" } });
    assert.equal(calls, 0);
  });

  test("an empty required field only gets `is required`", () => {
    assert.deepEqual(validate(signup, { password: "correct horse" }), {
      ok: false,
      errors: [{ field: "confirm", messages: ["is required"] }],
    });
  });

  test("a value that is not text only gets `must be text`", () => {
    assert.deepEqual(validate(signup, { password: "correct horse", confirm: "correct horse", nickname: 7 }), {
      ok: false,
      errors: [{ field: "nickname", messages: ["must be text"] }],
    });
  });
});

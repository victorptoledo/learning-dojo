import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { defineSchema, validate } from "../validator.ts";

const signup = defineSchema({
  name: [{ kind: "required" }, { kind: "minLength", min: 2 }],
  email: [{ kind: "required" }, { kind: "pattern", regex: /^[^@\s]+@[^@\s]+$/, message: "must be an email" }],
  age: [{ kind: "number", min: 18 }],
  plan: [{ kind: "oneOf", options: ["free", "pro"] }],
});

describe("defineSchema", () => {
  test("returns the schema it gets", () => {
    const schema = { name: [{ kind: "required" as const }] };
    assert.equal(defineSchema(schema), schema);
  });
});

describe("a valid form", () => {
  test("gives ok and the trimmed values of every field", () => {
    const result = validate(signup, { name: "  Ana ", email: "ana@example.com", age: "30", plan: "pro" });
    assert.deepEqual(result, {
      ok: true,
      values: { name: "Ana", email: "ana@example.com", age: "30", plan: "pro" },
    });
  });

  test("missing optional fields are empty text", () => {
    const result = validate(signup, { name: "Ana", email: "ana@example.com" });
    assert.deepEqual(result, { ok: true, values: { name: "Ana", email: "ana@example.com", age: "", plan: "" } });
  });

  test("fields outside the schema are ignored", () => {
    const result = validate(signup, { name: "Ana", email: "a@b", admin: "yes", extra: 1 });
    assert.equal(result.ok, true);
    if (result.ok) {
      assert.deepEqual(Object.keys(result.values).sort(), ["age", "email", "name", "plan"]);
    }
  });

  test("an empty schema is always valid", () => {
    assert.deepEqual(validate(defineSchema({}), { a: "1" }), { ok: true, values: {} });
  });
});

describe("an invalid form", () => {
  test("lists only the fields with messages, in the schema's order", () => {
    const result = validate(signup, { plan: "gold", age: "17", name: "A", email: "ana@example.com" });
    assert.deepEqual(result, {
      ok: false,
      errors: [
        { field: "name", messages: ["must be at least 2 characters"] },
        { field: "age", messages: ["must be at least 18"] },
        { field: "plan", messages: ["must be one of: free, pro"] },
      ],
    });
  });

  test("a field can have several messages", () => {
    const schema = defineSchema({
      code: [{ kind: "minLength", min: 4 }, { kind: "pattern", regex: /^\d+$/, message: "only digits" }],
    });
    assert.deepEqual(validate(schema, { code: "a1" }), {
      ok: false,
      errors: [{ field: "code", messages: ["must be at least 4 characters", "only digits"] }],
    });
  });

  test("values that are not text are reported, not converted", () => {
    const result = validate(signup, { name: "Ana", email: "a@b", age: 30 });
    assert.deepEqual(result, { ok: false, errors: [{ field: "age", messages: ["must be text"] }] });
  });

  test("null in a field is a missing value", () => {
    const result = validate(signup, { name: null, email: "a@b", age: null });
    assert.deepEqual(result, { ok: false, errors: [{ field: "name", messages: ["is required"] }] });
  });
});

describe("input that is not a plain object", () => {
  test("treats every field as missing", () => {
    for (const input of [null, undefined, "name=Ana", 42, ["Ana"], true]) {
      assert.deepEqual(
        validate(signup, input),
        {
          ok: false,
          errors: [
            { field: "name", messages: ["is required"] },
            { field: "email", messages: ["is required"] },
          ],
        },
        `input ${JSON.stringify(input)}`,
      );
    }
  });

  test("with only optional fields it is valid and empty", () => {
    const schema = defineSchema({ nickname: [{ kind: "maxLength", max: 10 }] });
    assert.deepEqual(validate(schema, null), { ok: true, values: { nickname: "" } });
  });

  test("works on data from JSON.parse", () => {
    const body: unknown = JSON.parse('{"name":"Bia","email":"bia@example.com","age":"21"}');
    assert.deepEqual(validate(signup, body), {
      ok: true,
      values: { name: "Bia", email: "bia@example.com", age: "21", plan: "" },
    });
  });

  test("inherited properties are not fields of the input", () => {
    const input: unknown = Object.create({ name: "Ana", email: "ana@example.com" });
    assert.deepEqual(validate(signup, input), {
      ok: false,
      errors: [
        { field: "name", messages: ["is required"] },
        { field: "email", messages: ["is required"] },
      ],
    });
  });
});

describe("the input", () => {
  test("is not changed", () => {
    const input = { name: "  Ana  ", email: "a@b" };
    validate(signup, input);
    assert.deepEqual(input, { name: "  Ana  ", email: "a@b" });
  });
});

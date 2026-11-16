import assert from "node:assert/strict";
import { test } from "node:test";
import { validateSignup } from "../signup.ts";

test("valid values have no errors", () => {
  const errors = validateSignup({
    name: "Ada",
    email: "ada@example.com",
    password: "engine42",
    confirm: "engine42",
    terms: true,
  });
  assert.deepEqual(errors, {});
});

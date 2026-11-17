import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { checkField, type Rule } from "../validator.ts";

const check = (rules: Rule[], value: unknown) => checkField(rules, value);

describe("required", () => {
  test("empty values break it", () => {
    for (const value of [undefined, null, "", "   ", "\t\n"]) {
      assert.deepEqual(check([{ kind: "required" }], value), ["is required"], `value ${JSON.stringify(value)}`);
    }
  });

  test("any text follows it", () => {
    assert.deepEqual(check([{ kind: "required" }], "a"), []);
    assert.deepEqual(check([{ kind: "required" }], " 0 "), []);
  });

  test("an empty required field gets only that message", () => {
    const rules: Rule[] = [{ kind: "minLength", min: 3 }, { kind: "required" }, { kind: "number" }];
    assert.deepEqual(check(rules, "  "), ["is required"]);
  });
});

describe("empty optional fields", () => {
  test("are valid whatever the other rules", () => {
    const rules: Rule[] = [
      { kind: "minLength", min: 3 },
      { kind: "pattern", regex: /^x+$/, message: "only x" },
      { kind: "number", min: 1 },
      { kind: "oneOf", options: ["a"] },
    ];
    for (const value of [undefined, null, "", "  "]) {
      assert.deepEqual(check(rules, value), [], `value ${JSON.stringify(value)}`);
    }
  });

  test("no rules at all is valid", () => {
    assert.deepEqual(check([], "anything"), []);
  });
});

describe("values that are not text", () => {
  test("get the single message `must be text`", () => {
    for (const value of [42, true, {}, ["a"], 0]) {
      assert.deepEqual(check([{ kind: "required" }, { kind: "number" }], value), ["must be text"]);
      assert.deepEqual(check([], value), ["must be text"]);
    }
  });
});

describe("lengths", () => {
  test("minLength and maxLength count the trimmed text", () => {
    assert.deepEqual(check([{ kind: "minLength", min: 3 }], "  ab  "), ["must be at least 3 characters"]);
    assert.deepEqual(check([{ kind: "minLength", min: 3 }], "abc"), []);
    assert.deepEqual(check([{ kind: "maxLength", max: 4 }], " abcd "), []);
    assert.deepEqual(check([{ kind: "maxLength", max: 4 }], "abcde"), ["must be at most 4 characters"]);
  });

  test("an emoji is one character", () => {
    assert.deepEqual(check([{ kind: "minLength", min: 3 }], "😀😀"), ["must be at least 3 characters"]);
    assert.deepEqual(check([{ kind: "maxLength", max: 2 }], "😀😀"), []);
  });
});

describe("pattern", () => {
  const zip: Rule = { kind: "pattern", regex: /^\d{5}-\d{3}$/, message: "must look like 01310-100" };

  test("uses the rule's message", () => {
    assert.deepEqual(check([zip], "01310100"), ["must look like 01310-100"]);
    assert.deepEqual(check([zip], " 01310-100 "), []);
  });

  test("a global regex gives the same answer every time", () => {
    const rule: Rule = { kind: "pattern", regex: /a/g, message: "needs an a" };
    assert.deepEqual(check([rule], "a"), []);
    assert.deepEqual(check([rule], "a"), []);
    assert.deepEqual(check([rule], "a"), []);
  });
});

describe("number", () => {
  test("accepts integers and decimals, with an optional minus", () => {
    for (const value of ["42", "-3", "2.5", "0", " 7 ", "-0.25"]) {
      assert.deepEqual(check([{ kind: "number" }], value), [], `value ${value}`);
    }
  });

  test("anything else is not a number", () => {
    for (const value of ["1e3", "0x10", ".5", "12abc", "5.", "+5", "1,5", "--1", "Infinity", "NaN"]) {
      assert.deepEqual(check([{ kind: "number" }], value), ["must be a number"], `value ${value}`);
    }
  });

  test("min and max are inclusive", () => {
    const age: Rule = { kind: "number", min: 18, max: 120 };
    assert.deepEqual(check([age], "18"), []);
    assert.deepEqual(check([age], "120"), []);
    assert.deepEqual(check([age], "17.5"), ["must be at least 18"]);
    assert.deepEqual(check([age], "121"), ["must be at most 120"]);
  });

  test("min and max are optional, and 0 is a real bound", () => {
    assert.deepEqual(check([{ kind: "number", max: 0 }], "1"), ["must be at most 0"]);
    assert.deepEqual(check([{ kind: "number", min: 0 }], "-1"), ["must be at least 0"]);
    assert.deepEqual(check([{ kind: "number", min: 0 }], "100000"), []);
  });

  test("a value that is not a number only gets `must be a number`", () => {
    assert.deepEqual(check([{ kind: "number", min: 1, max: 2 }], "abc"), ["must be a number"]);
  });
});

describe("oneOf", () => {
  const size: Rule = { kind: "oneOf", options: ["S", "M", "L"] };

  test("lists the options in the message", () => {
    assert.deepEqual(check([size], "XL"), ["must be one of: S, M, L"]);
    assert.deepEqual(check([size], " M "), []);
  });

  test("compares exactly, case included", () => {
    assert.deepEqual(check([size], "m"), ["must be one of: S, M, L"]);
  });
});

describe("several rules", () => {
  test("every broken rule gives its message, in the order of the rules", () => {
    const rules: Rule[] = [
      { kind: "required" },
      { kind: "pattern", regex: /^[a-z]+$/, message: "only lowercase letters" },
      { kind: "minLength", min: 5 },
      { kind: "maxLength", max: 10 },
    ];
    assert.deepEqual(check(rules, "AB"), ["only lowercase letters", "must be at least 5 characters"]);
    assert.deepEqual(check(rules, "hello"), []);
  });

  test("the rules are not changed", () => {
    const rules: Rule[] = [{ kind: "oneOf", options: ["a", "b"] }];
    const copy = structuredClone(rules);
    check(rules, "c");
    assert.deepEqual(rules, copy);
  });
});

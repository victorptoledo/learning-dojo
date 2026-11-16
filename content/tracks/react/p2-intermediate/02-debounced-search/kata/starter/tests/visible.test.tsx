import assert from "node:assert/strict";
import { test } from "node:test";
import { renderHook } from "@testing-library/react";
import { useDebouncedValue } from "../useDebouncedValue.ts";

test("the first value is returned right away", () => {
  const { result } = renderHook(() => useDebouncedValue("react", 50));
  assert.equal(result.current, "react");
});

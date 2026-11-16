import assert from "node:assert/strict";
import { test } from "node:test";
import { render, screen } from "@testing-library/react";
import { Badge } from "../Badge.tsx";

test("a badge shows its label with the neutral tone by default", () => {
  render(<Badge label="Admin" />);
  const badge = screen.getByText("Admin");
  assert.equal(badge.tagName, "SPAN");
  assert.equal(badge.className, "badge badge-neutral");
});

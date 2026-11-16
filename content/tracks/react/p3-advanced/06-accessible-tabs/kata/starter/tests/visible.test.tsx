import assert from "node:assert/strict";
import { test } from "node:test";
import { render, screen } from "@testing-library/react";
import { Tabs } from "../Tabs.tsx";

test("renders a named tab list with one tab per item", () => {
  render(
    <Tabs
      label="Settings"
      tabs={[
        { id: "a", title: "Profile", content: "Profile form" },
        { id: "b", title: "Security", content: "Security form" },
      ]}
    />,
  );
  assert.ok(screen.getByRole("tablist", { name: "Settings" }));
  assert.equal(screen.getAllByRole("tab").length, 2);
});

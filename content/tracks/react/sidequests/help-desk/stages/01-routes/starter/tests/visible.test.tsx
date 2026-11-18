// Your own tests: `npm test` compiles the project and runs every tests/*.test.tsx. Add more as you go.
import assert from "node:assert/strict";
import { test } from "node:test";
import { act, render, screen } from "@testing-library/react";
import { HelpDesk } from "../HelpDesk.tsx";
import type { HelpDeskApi } from "../helpdesk-types.ts";

test("the app opens on the overview", async () => {
  window.history.replaceState(null, "", "/");
  const api: HelpDeskApi = {
    listTickets: async () => [],
    getTicket: async () => {
      throw new Error("not used here");
    },
    reply: async () => {
      throw new Error("not used here");
    },
    setStatus: async () => {
      throw new Error("not used here");
    },
  };
  render(<HelpDesk api={api} />);
  await act(async () => {});
  assert.ok(screen.getByRole("heading", { level: 1, name: "Overview" }));
});

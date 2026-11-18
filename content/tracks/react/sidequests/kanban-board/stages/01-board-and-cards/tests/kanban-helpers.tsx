// Helpers shared by the contract tests of every stage. They render the board the way an app
// would and read it only through roles, names and visible text.
import assert from "node:assert/strict";
import { act, render, screen, within } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import type { ReactNode } from "react";
import { Board, BoardProvider, BoardSummary } from "../Board.tsx";
import type { BoardApi, BoardData } from "../board-types.ts";

export type User = ReturnType<typeof userEvent.setup>;

function deepFreeze<T>(value: T): T {
  if (value && typeof value === "object") {
    for (const inner of Object.values(value)) {
      deepFreeze(inner);
    }
    Object.freeze(value);
  }
  return value;
}

/** A fresh board each time, frozen: changing it in place throws. */
export function sampleBoard(): BoardData {
  return deepFreeze({
    columns: [
      {
        id: "todo",
        title: "To do",
        cards: [
          { id: "a", title: "Write tests" },
          { id: "b", title: "Fix bug" },
        ],
      },
      { id: "doing", title: "Doing", cards: [{ id: "c", title: "Review PR" }] },
      { id: "done", title: "Done", cards: [] },
    ],
  });
}

export function start(options: { initial?: BoardData; api?: BoardApi; extra?: ReactNode } = {}) {
  let next = 1;
  const user = userEvent.setup();
  const view = render(
    <BoardProvider initial={options.initial ?? sampleBoard()} createId={() => `new-${next++}`} api={options.api}>
      <main>
        <Board />
      </main>
      <aside>
        <BoardSummary />
      </aside>
      {options.extra}
    </BoardProvider>,
  );
  return { user, ...view };
}

export const column = (title: string) => screen.getByRole("region", { name: title });

/** The card titles of a column, in order, read from their Edit buttons. */
export function cardsIn(title: string): string[] {
  return within(column(title))
    .queryAllByRole("button", { name: /^Edit / })
    .map((button) => (button.textContent ?? "").replace(/^Edit\s+/, "").trim());
}

export function board(): Record<string, string[]> {
  return { "To do": cardsIn("To do"), Doing: cardsIn("Doing"), Done: cardsIn("Done") };
}

/** The Summary lines, e.g. ["To do: 2", "Doing: 1", "Done: 0", "3 cards in total"]. */
export function summary(): string[] {
  const region = screen.getByRole("region", { name: "Summary" });
  const lines = within(region)
    .getAllByRole("listitem")
    .map((item) => (item.textContent ?? "").trim());
  return [...lines, (within(region).getByText(/in total$/).textContent ?? "").trim()];
}

export async function addCard(user: User, columnTitle: string, title: string): Promise<void> {
  const region = column(columnTitle);
  const box = within(region).getByRole("textbox", { name: `New card in ${columnTitle}` });
  await user.clear(box);
  if (title !== "") {
    await user.type(box, title);
  }
  await user.click(within(region).getByRole("button", { name: "Add card" }));
}

export const announcement = () => (screen.getByRole("status").textContent ?? "").trim();

export const moveButton = (title: string) => screen.getByRole("button", { name: `Move ${title}` });

/** Focuses a card's Move button and presses the keys, e.g. carry(user, "Fix bug", " {ArrowRight} "). */
export async function carry(user: User, title: string, keys: string): Promise<void> {
  moveButton(title).focus();
  await user.keyboard(keys);
}

export type SaveCall = { board: BoardData; ok(): Promise<void>; fail(): Promise<void> };

export function fakeApi(): { api: BoardApi; calls: SaveCall[] } {
  const calls: SaveCall[] = [];
  const api: BoardApi = {
    save(saved) {
      return new Promise<void>((resolve, reject) => {
        calls.push({
          board: JSON.parse(JSON.stringify(saved)) as BoardData,
          ok: () => act(async () => resolve()),
          fail: () => act(async () => reject(new Error("HTTP 503"))),
        });
      });
    },
  };
  return { api, calls };
}

/** The card titles of a saved board, per column title. */
export function titlesOf(saved: BoardData): Record<string, string[]> {
  return Object.fromEntries(saved.columns.map((col) => [col.title, col.cards.map((card) => card.title)]));
}

/** A short description of an element for failure messages (never the element itself). */
function nameOf(element: Element | null): string {
  if (!element || element === document.body) {
    return "nothing (the page itself)";
  }
  const label =
    element.getAttribute("aria-label") ??
    ("labels" in element ? (element as HTMLInputElement).labels?.[0]?.textContent : null) ??
    element.textContent ??
    "";
  return `<${element.tagName.toLowerCase()}> "${label.replace(/\s+/g, " ").trim().slice(0, 60)}"`;
}

// Compare elements with these, not with assert.equal: when it fails, assert would print the
// whole document, which takes minutes.
export function expectFocus(element: Element, context?: string): void {
  const active = document.activeElement;
  if (active !== element) {
    assert.fail(`${context ? `${context}: ` : ""}the focus should be on ${nameOf(element)}, but it is on ${nameOf(active)}`);
  }
}

export function expectAbsent(element: Element | null, context?: string): void {
  if (element !== null) {
    assert.fail(`${context ? `${context}: ` : ""}${nameOf(element)} should not be on the page`);
  }
}

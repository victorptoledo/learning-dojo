import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { render, screen, within } from "@testing-library/react";
import { Component, type ReactNode } from "react";
import { Board, BoardSummary } from "../Board.tsx";
import { addCard, board, cardsIn, column, start, summary } from "./kanban-helpers.tsx";
import { expectAbsent, expectFocus } from "./kanban-helpers.tsx";

class Catch extends Component<{ children: ReactNode }, { message: string | null }> {
  override state = { message: null as string | null };
  static getDerivedStateFromError(error: unknown) {
    return { message: error instanceof Error ? error.message : String(error) };
  }
  override render() {
    return this.state.message === null ? this.props.children : <p>{`caught: ${this.state.message}`}</p>;
  }
}

describe("the board", () => {
  test("shows each column as a region with its cards, in order", () => {
    start();
    const regions = screen.getAllByRole("region").filter((region) => region.closest("main"));
    assert.deepEqual(
      regions.map((region) => within(region).getByRole("heading").textContent),
      ["To do", "Doing", "Done"],
    );
    assert.deepEqual(board(), { "To do": ["Write tests", "Fix bug"], Doing: ["Review PR"], Done: [] });
    assert.ok(within(column("Done")).getByText("No cards"));
    expectAbsent(within(column("To do")).queryByText("No cards"));
  });

  test("the summary, rendered elsewhere, counts the cards of each column", () => {
    start();
    assert.deepEqual(summary(), ["To do: 2", "Doing: 1", "Done: 0", "3 cards in total"]);
  });

  test("one card is one card", () => {
    start({ initial: { columns: [{ id: "x", title: "Only", cards: [{ id: "1", title: "Alone" }] }] } });
    assert.deepEqual(summary(), ["Only: 1", "1 card in total"]);
  });

  test("using the board outside a BoardProvider fails with a clear message", () => {
    const original = console.error;
    console.error = () => {};
    try {
      render(
        <Catch>
          <Board />
        </Catch>,
      );
      assert.match(screen.getByText(/^caught:/).textContent ?? "", /BoardProvider/);
    } finally {
      console.error = original;
    }
  });

  test("so does the summary", () => {
    const original = console.error;
    console.error = () => {};
    try {
      render(
        <Catch>
          <BoardSummary />
        </Catch>,
      );
      assert.match(screen.getByText(/^caught:/).textContent ?? "", /BoardProvider/);
    } finally {
      console.error = original;
    }
  });
});

describe("adding cards", () => {
  test("a card goes at the end of its column, and the summary follows", async () => {
    const { user } = start();
    await addCard(user, "To do", "  Deploy  ");
    assert.deepEqual(cardsIn("To do"), ["Write tests", "Fix bug", "Deploy"]);
    assert.deepEqual(summary(), ["To do: 3", "Doing: 1", "Done: 0", "4 cards in total"]);
  });

  test("an empty column takes its first card", async () => {
    const { user } = start();
    await addCard(user, "Done", "Ship it");
    assert.deepEqual(cardsIn("Done"), ["Ship it"]);
    expectAbsent(within(column("Done")).queryByText("No cards"));
  });

  test("the field is emptied and keeps the focus; Enter adds too", async () => {
    const { user } = start();
    const box = within(column("Doing")).getByRole("textbox", { name: "New card in Doing" }) as HTMLInputElement;
    await user.type(box, "Pair{Enter}");
    assert.equal(box.value, "");
    expectFocus(box);
    assert.deepEqual(cardsIn("Doing"), ["Review PR", "Pair"]);
  });

  test("a blank title adds nothing", async () => {
    const { user } = start();
    await addCard(user, "To do", "   ");
    assert.deepEqual(cardsIn("To do"), ["Write tests", "Fix bug"]);
  });
});

describe("editing and deleting", () => {
  test("Edit turns the card into a field with its title, focused", async () => {
    const { user } = start();
    await user.click(screen.getByRole("button", { name: "Edit Fix bug" }));
    const box = screen.getByRole("textbox", { name: "Title of Fix bug" }) as HTMLInputElement;
    assert.equal(box.value, "Fix bug");
    expectFocus(box);
    assert.ok(screen.getByRole("button", { name: "Save" }));
    assert.ok(screen.getByRole("button", { name: "Cancel" }));
  });

  test("Enter saves the new title and gives the focus back to its Edit button", async () => {
    const { user } = start();
    await user.click(screen.getByRole("button", { name: "Edit Fix bug" }));
    await user.clear(screen.getByRole("textbox", { name: "Title of Fix bug" }));
    await user.keyboard(" Fix the login bug {Enter}");
    assert.deepEqual(cardsIn("To do"), ["Write tests", "Fix the login bug"]);
    expectFocus(screen.getByRole("button", { name: "Edit Fix the login bug" }));
  });

  test("the Save button saves too", async () => {
    const { user } = start();
    await user.click(screen.getByRole("button", { name: "Edit Review PR" }));
    await user.type(screen.getByRole("textbox", { name: "Title of Review PR" }), " #42");
    await user.click(screen.getByRole("button", { name: "Save" }));
    assert.deepEqual(cardsIn("Doing"), ["Review PR #42"]);
  });

  test("Escape and Cancel keep the old title", async () => {
    const { user } = start();
    await user.click(screen.getByRole("button", { name: "Edit Fix bug" }));
    await user.type(screen.getByRole("textbox", { name: "Title of Fix bug" }), " later");
    await user.keyboard("{Escape}");
    assert.deepEqual(cardsIn("To do"), ["Write tests", "Fix bug"]);
    expectFocus(screen.getByRole("button", { name: "Edit Fix bug" }));
    await user.click(screen.getByRole("button", { name: "Edit Fix bug" }));
    await user.type(screen.getByRole("textbox", { name: "Title of Fix bug" }), " later");
    await user.click(screen.getByRole("button", { name: "Cancel" }));
    assert.deepEqual(cardsIn("To do"), ["Write tests", "Fix bug"]);
  });

  test("a blank title keeps the old one", async () => {
    const { user } = start();
    await user.click(screen.getByRole("button", { name: "Edit Fix bug" }));
    await user.clear(screen.getByRole("textbox", { name: "Title of Fix bug" }));
    await user.keyboard("{Enter}");
    assert.deepEqual(cardsIn("To do"), ["Write tests", "Fix bug"]);
  });

  test("Delete removes the card and moves the focus to its column's field", async () => {
    const { user } = start();
    await user.click(screen.getByRole("button", { name: "Delete Write tests" }));
    assert.deepEqual(cardsIn("To do"), ["Fix bug"]);
    assert.deepEqual(summary(), ["To do: 1", "Doing: 1", "Done: 0", "2 cards in total"]);
    expectFocus(within(column("To do")).getByRole("textbox", { name: "New card in To do" }));
  });

  test("the initial board is never changed in place", async () => {
    const { user } = start();
    await addCard(user, "To do", "One more");
    await user.click(screen.getByRole("button", { name: "Delete Review PR" }));
    await user.click(screen.getByRole("button", { name: "Edit Fix bug" }));
    await user.type(screen.getByRole("textbox", { name: "Title of Fix bug" }), "!{Enter}");
    assert.deepEqual(board(), { "To do": ["Write tests", "Fix bug!", "One more"], Doing: [], Done: [] });
  });
});

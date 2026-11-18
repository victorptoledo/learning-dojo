import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { addCard, announcement, board, carry, cardsIn, column, start, summary, type User } from "./kanban-helpers.tsx";

const history = () => screen.getByRole("toolbar", { name: "History" });
const undoButton = () => within(history()).getByRole("button", { name: "Undo" }) as HTMLButtonElement;
const redoButton = () => within(history()).getByRole("button", { name: "Redo" }) as HTMLButtonElement;
const ORIGINAL = { "To do": ["Write tests", "Fix bug"], Doing: ["Review PR"], Done: [] };

async function rename(user: User, from: string, to: string): Promise<void> {
  await user.click(screen.getByRole("button", { name: `Edit ${from}` }));
  const box = screen.getByRole("textbox", { name: `Title of ${from}` });
  await user.clear(box);
  await user.type(box, `${to}{Enter}`);
}

describe("undo and redo", () => {
  test("there is nothing to undo or redo at first", () => {
    start();
    assert.equal(undoButton().disabled, true);
    assert.equal(redoButton().disabled, true);
  });

  test("adding a card can be undone and redone, and both are announced", async () => {
    const { user } = start();
    await addCard(user, "Done", "Ship");
    assert.equal(undoButton().disabled, false);
    await user.click(undoButton());
    assert.deepEqual(board(), ORIGINAL);
    assert.equal(announcement(), "Undid adding Ship.");
    assert.equal(undoButton().disabled, true);
    assert.equal(redoButton().disabled, false);
    await user.click(redoButton());
    assert.deepEqual(cardsIn("Done"), ["Ship"]);
    assert.equal(announcement(), "Redid adding Ship.");
    assert.deepEqual(summary(), ["To do: 2", "Doing: 1", "Done: 1", "4 cards in total"]);
  });

  test("deleting and renaming are steps too, undone in reverse order", async () => {
    const { user } = start();
    await user.click(screen.getByRole("button", { name: "Delete Review PR" }));
    await rename(user, "Fix bug", "Fix login");
    await user.click(undoButton());
    assert.equal(announcement(), "Undid renaming Fix bug to Fix login.");
    assert.deepEqual(cardsIn("To do"), ["Write tests", "Fix bug"]);
    await user.click(undoButton());
    assert.equal(announcement(), "Undid deleting Review PR.");
    assert.deepEqual(board(), ORIGINAL);
  });

  test("a whole move, from pick-up to drop, is one step", async () => {
    const { user } = start();
    await carry(user, "Write tests", " {ArrowRight}{ArrowRight}{ArrowLeft}{ArrowDown} ");
    assert.deepEqual(board().Doing, ["Review PR", "Write tests"]);
    await user.click(undoButton());
    assert.deepEqual(board(), ORIGINAL);
    assert.equal(announcement(), "Undid moving Write tests.");
    assert.equal(undoButton().disabled, true);
  });

  test("a cancelled move, or one dropped where it started, is no step", async () => {
    const { user } = start();
    await carry(user, "Fix bug", " {ArrowRight}{Escape}");
    await carry(user, "Fix bug", " {ArrowRight}{ArrowLeft} ");
    assert.equal(undoButton().disabled, true);
  });

  test("undo and redo are disabled while a card is carried", async () => {
    const { user } = start();
    await addCard(user, "Done", "Ship");
    await carry(user, "Fix bug", " ");
    assert.equal(undoButton().disabled, true);
    await user.keyboard("{Control>}z{/Control}");
    assert.deepEqual(cardsIn("Done"), ["Ship"]);
  });

  test("a new change forgets what could be redone", async () => {
    const { user } = start();
    await addCard(user, "Done", "Ship");
    await user.click(undoButton());
    await addCard(user, "Doing", "Pair");
    assert.equal(redoButton().disabled, true);
  });

  test("Ctrl+Z undoes, Ctrl+Y and Ctrl+Shift+Z redo, from anywhere on the board", async () => {
    const { user } = start();
    await user.click(screen.getByRole("button", { name: "Delete Fix bug" }));
    screen.getByRole("button", { name: "Edit Write tests" }).focus();
    await user.keyboard("{Control>}z{/Control}");
    assert.deepEqual(board(), ORIGINAL);
    await user.keyboard("{Control>}y{/Control}");
    assert.deepEqual(cardsIn("To do"), ["Write tests"]);
    await user.keyboard("{Control>}z{/Control}");
    await user.keyboard("{Control>}{Shift>}z{/Shift}{/Control}");
    assert.deepEqual(cardsIn("To do"), ["Write tests"]);
  });

  test("Ctrl+Z in a text field is left to the field", async () => {
    const { user } = start();
    await addCard(user, "Done", "Ship");
    const box = within(column("Doing")).getByRole("textbox", { name: "New card in Doing" });
    await user.click(box);
    await user.keyboard("{Control>}z{/Control}");
    assert.deepEqual(cardsIn("Done"), ["Ship"]);
  });

  test("it keeps the last 20 steps", async () => {
    const { user } = start();
    for (let index = 1; index <= 22; index++) {
      await addCard(user, "Done", `Card ${index}`);
    }
    let undone = 0;
    while (!undoButton().disabled) {
      await user.click(undoButton());
      undone += 1;
    }
    assert.equal(undone, 20);
    assert.deepEqual(cardsIn("Done"), ["Card 1", "Card 2"]);
  });
});

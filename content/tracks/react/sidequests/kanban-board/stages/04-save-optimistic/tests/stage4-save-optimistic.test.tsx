import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { screen, within } from "@testing-library/react";
import { addCard, board, carry, cardsIn, fakeApi, sampleBoard, start, titlesOf } from "./kanban-helpers.tsx";
import { expectAbsent } from "./kanban-helpers.tsx";

const ORIGINAL = { "To do": ["Write tests", "Fix bug"], Doing: ["Review PR"], Done: [] };
const saved = () => screen.queryByText("All changes saved");
const saving = () => screen.queryByText("Saving…");
const undoButton = () =>
  within(screen.getByRole("toolbar", { name: "History" })).getByRole("button", { name: "Undo" }) as HTMLButtonElement;

describe("saving", () => {
  test("nothing is saved before the first change", () => {
    const { api, calls } = fakeApi();
    start({ api });
    assert.equal(calls.length, 0);
    assert.ok(saved());
    expectAbsent(saving());
  });

  test("a change shows at once and saves the whole board", async () => {
    const { api, calls } = fakeApi();
    const { user } = start({ api });
    await addCard(user, "Done", "Ship");
    assert.deepEqual(cardsIn("Done"), ["Ship"]);
    assert.equal(calls.length, 1);
    assert.deepEqual(titlesOf(calls[0]!.board), { ...ORIGINAL, Done: ["Ship"] });
    assert.deepEqual(
      calls[0]!.board.columns.map((col) => col.id),
      sampleBoard().columns.map((col) => col.id),
    );
    assert.ok(saving());
    expectAbsent(saved());
    await calls[0]!.ok();
    assert.ok(saved());
    expectAbsent(saving());
  });

  test("one save at a time: changes made meanwhile go together in the next one", async () => {
    const { api, calls } = fakeApi();
    const { user } = start({ api });
    await addCard(user, "Done", "A");
    await addCard(user, "Done", "B");
    await user.click(screen.getByRole("button", { name: "Delete Review PR" }));
    assert.equal(calls.length, 1);
    await calls[0]!.ok();
    assert.equal(calls.length, 2);
    assert.deepEqual(titlesOf(calls[1]!.board), { ...ORIGINAL, Doing: [], Done: ["A", "B"] });
    assert.ok(saving());
    await calls[1]!.ok();
    assert.equal(calls.length, 2);
    assert.ok(saved());
  });

  test("a move is saved when it is dropped, not on every key", async () => {
    const { api, calls } = fakeApi();
    const { user } = start({ api });
    await carry(user, "Fix bug", " {ArrowRight}{ArrowRight}");
    assert.equal(calls.length, 0);
    await user.keyboard(" ");
    assert.equal(calls.length, 1);
    assert.deepEqual(titlesOf(calls[0]!.board), { "To do": ["Write tests"], Doing: ["Review PR"], Done: ["Fix bug"] });
  });

  test("a cancelled move saves nothing", async () => {
    const { api, calls } = fakeApi();
    const { user } = start({ api });
    await carry(user, "Fix bug", " {ArrowRight}{Escape}");
    assert.equal(calls.length, 0);
  });

  test("undo is a change like any other", async () => {
    const { api, calls } = fakeApi();
    const { user } = start({ api });
    await addCard(user, "Done", "Ship");
    await calls[0]!.ok();
    await user.click(undoButton());
    assert.equal(calls.length, 2);
    assert.deepEqual(titlesOf(calls[1]!.board), ORIGINAL);
  });
});

describe("when saving fails", () => {
  test("the board goes back to the last saved version and says so", async () => {
    const { api, calls } = fakeApi();
    const { user } = start({ api });
    await addCard(user, "Done", "Kept");
    await calls[0]!.ok();
    await addCard(user, "Done", "Lost");
    await calls[1]!.fail();
    assert.deepEqual(cardsIn("Done"), ["Kept"]);
    const alert = screen.getByRole("alert");
    assert.ok(within(alert).getByText("Could not save. The board was restored to the last saved version."));
    expectAbsent(saving());
  });

  test("changes waiting behind the failed save are dropped too", async () => {
    const { api, calls } = fakeApi();
    const { user } = start({ api });
    await addCard(user, "Done", "First");
    await addCard(user, "Done", "Second");
    await calls[0]!.fail();
    assert.deepEqual(board(), ORIGINAL);
    assert.equal(calls.length, 1, "the restored board is the saved one: nothing to save");
  });

  test("the history is cleared, since it held changes that were never saved", async () => {
    const { api, calls } = fakeApi();
    const { user } = start({ api });
    await addCard(user, "Done", "Lost");
    await calls[0]!.fail();
    assert.equal(undoButton().disabled, true);
  });

  test("Dismiss closes the alert, and the board keeps working", async () => {
    const { api, calls } = fakeApi();
    const { user } = start({ api });
    await addCard(user, "Done", "Lost");
    await calls[0]!.fail();
    await user.click(within(screen.getByRole("alert")).getByRole("button", { name: "Dismiss" }));
    expectAbsent(screen.queryByRole("alert"));
    await addCard(user, "Doing", "Retry");
    assert.equal(calls.length, 2);
    assert.deepEqual(titlesOf(calls[1]!.board), { ...ORIGINAL, Doing: ["Review PR", "Retry"] });
    await calls[1]!.ok();
    assert.ok(saved());
  });
});

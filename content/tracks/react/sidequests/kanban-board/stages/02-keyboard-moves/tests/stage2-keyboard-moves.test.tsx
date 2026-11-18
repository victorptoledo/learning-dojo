import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { announcement, board, carry, moveButton, start, summary } from "./kanban-helpers.tsx";
import { expectFocus } from "./kanban-helpers.tsx";

const INSTRUCTIONS = "Press Space to pick up a card, the arrow keys to move it, Space to drop it and Escape to cancel.";

function describedBy(element: HTMLElement): string {
  return (element.getAttribute("aria-describedby") ?? "")
    .split(/\s+/)
    .filter(Boolean)
    .map((id) => document.getElementById(id)?.textContent?.trim() ?? "")
    .join(" ");
}

describe("picking up a card", () => {
  test("every card has a Move button that explains the keys", () => {
    start();
    for (const title of ["Write tests", "Fix bug", "Review PR"]) {
      assert.equal(describedBy(moveButton(title)), INSTRUCTIONS, title);
      assert.equal(moveButton(title).getAttribute("aria-pressed"), "false", title);
    }
  });

  test("Space picks it up and says where it is", async () => {
    const { user } = start();
    await carry(user, "Write tests", " ");
    assert.equal(moveButton("Write tests").getAttribute("aria-pressed"), "true");
    assert.equal(announcement(), "Picked up Write tests in To do, position 1 of 2.");
  });

  test("Enter picks it up too, and a click as well", async () => {
    const { user } = start();
    await carry(user, "Fix bug", "{Enter}");
    assert.equal(announcement(), "Picked up Fix bug in To do, position 2 of 2.");
    await user.keyboard("{Enter}");
    await user.click(moveButton("Review PR"));
    assert.equal(moveButton("Review PR").getAttribute("aria-pressed"), "true");
  });

  test("arrow keys do nothing to a card that was not picked up", async () => {
    const { user } = start();
    await carry(user, "Write tests", "{ArrowDown}{ArrowRight}");
    assert.deepEqual(board(), { "To do": ["Write tests", "Fix bug"], Doing: ["Review PR"], Done: [] });
  });
});

describe("moving it", () => {
  test("up and down within its column, announced", async () => {
    const { user } = start();
    await carry(user, "Write tests", " {ArrowDown}");
    assert.deepEqual(board()["To do"], ["Fix bug", "Write tests"]);
    assert.equal(announcement(), "Write tests is in To do, position 2 of 2.");
    await user.keyboard("{ArrowUp}");
    assert.deepEqual(board()["To do"], ["Write tests", "Fix bug"]);
    assert.equal(announcement(), "Write tests is in To do, position 1 of 2.");
  });

  test("left and right to the next column, at the same position or at its end", async () => {
    const { user } = start();
    await carry(user, "Fix bug", " {ArrowRight}");
    assert.deepEqual(board(), { "To do": ["Write tests"], Doing: ["Review PR", "Fix bug"], Done: [] });
    assert.equal(announcement(), "Fix bug is in Doing, position 2 of 2.");
    await user.keyboard("{ArrowRight}");
    assert.deepEqual(board().Done, ["Fix bug"]);
    assert.equal(announcement(), "Fix bug is in Done, position 1 of 1.");
    await user.keyboard("{ArrowLeft}");
    assert.deepEqual(board().Doing, ["Fix bug", "Review PR"]);
    assert.equal(announcement(), "Fix bug is in Doing, position 1 of 2.");
    await user.keyboard("{ArrowLeft}");
    assert.deepEqual(board()["To do"], ["Fix bug", "Write tests"]);
  });

  test("a card moving to the first position of another column", async () => {
    const { user } = start();
    await carry(user, "Write tests", " {ArrowRight}");
    assert.deepEqual(board().Doing, ["Write tests", "Review PR"]);
    assert.equal(announcement(), "Write tests is in Doing, position 1 of 2.");
  });

  test("the focus stays on the card it carries, in any column", async () => {
    const { user } = start();
    await carry(user, "Write tests", " {ArrowDown}");
    expectFocus(moveButton("Write tests"));
    await user.keyboard("{ArrowRight}");
    expectFocus(moveButton("Write tests"));
    await user.keyboard("{ArrowRight}");
    expectFocus(moveButton("Write tests"));
  });

  test("at the edges nothing moves", async () => {
    const { user } = start();
    await carry(user, "Write tests", " {ArrowUp}{ArrowLeft}");
    assert.deepEqual(board()["To do"], ["Write tests", "Fix bug"]);
    assert.equal(announcement(), "Picked up Write tests in To do, position 1 of 2.");
    await user.keyboard("{Escape}");
    await carry(user, "Review PR", " {ArrowDown}{ArrowRight}{ArrowRight}");
    assert.deepEqual(board().Done, ["Review PR"]);
  });
});

describe("dropping or cancelling", () => {
  test("Space drops it where it is and says so", async () => {
    const { user } = start();
    await carry(user, "Write tests", " {ArrowRight}{ArrowDown} ");
    assert.deepEqual(board(), { "To do": ["Fix bug"], Doing: ["Review PR", "Write tests"], Done: [] });
    assert.equal(announcement(), "Dropped Write tests in Doing, position 2 of 2.");
    assert.equal(moveButton("Write tests").getAttribute("aria-pressed"), "false");
    expectFocus(moveButton("Write tests"));
    assert.deepEqual(summary(), ["To do: 1", "Doing: 2", "Done: 0", "3 cards in total"]);
  });

  test("after a drop the arrow keys no longer move it", async () => {
    const { user } = start();
    await carry(user, "Write tests", " {ArrowDown} {ArrowUp}{ArrowRight}");
    assert.deepEqual(board()["To do"], ["Fix bug", "Write tests"]);
  });

  test("Escape puts it back where it was", async () => {
    const { user } = start();
    await carry(user, "Fix bug", " {ArrowUp}{ArrowRight}{ArrowRight}{Escape}");
    assert.deepEqual(board(), { "To do": ["Write tests", "Fix bug"], Doing: ["Review PR"], Done: [] });
    assert.equal(announcement(), "Cancelled. Fix bug is back in To do, position 2 of 2.");
    assert.equal(moveButton("Fix bug").getAttribute("aria-pressed"), "false");
    expectFocus(moveButton("Fix bug"));
  });

  test("one card at a time: picking another one is not possible while carrying", async () => {
    const { user } = start();
    await carry(user, "Write tests", " {ArrowRight}");
    await user.click(moveButton("Review PR"));
    assert.equal(moveButton("Review PR").getAttribute("aria-pressed"), "false");
  });
});

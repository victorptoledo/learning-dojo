import assert from "node:assert/strict";
import { test } from "node:test";
import { checkNewComment } from "../server.ts";

test("a signed-in user's comment is accepted, trimmed, with the author taken from the session", () => {
  const form = new FormData();
  form.set("postId", "p1");
  form.set("body", "  Great post!  ");
  const result = checkNewComment({ userId: "u7", name: "Ana" }, form, new Set(["p1"]));
  assert.deepEqual(result, {
    ok: true,
    comment: { postId: "p1", authorId: "u7", authorName: "Ana", body: "Great post!" },
  });
});

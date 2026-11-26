import assert from "node:assert/strict";
import { test } from "node:test";
import { createClient, defineFlags } from "@dojo/flags";

test("a refresh changes a flag and tells its listener", async () => {
  const flags = defineFlags({
    theme: { kind: "variant", variants: ["light", "dark"], default: "light" },
  });
  const client = createClient(flags);
  const seen: string[] = [];
  client.subscribe("theme", (value, previous) => seen.push(`${previous} -> ${value}`));
  const result = await client.refresh(async () => ({ theme: "dark" }));
  assert.deepEqual(result, { changed: ["theme"], stale: false });
  assert.equal(client.get("theme"), "dark");
  assert.deepEqual(seen, ["light -> dark"]);
});

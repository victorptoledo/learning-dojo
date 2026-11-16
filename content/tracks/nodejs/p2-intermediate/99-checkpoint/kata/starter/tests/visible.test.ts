import assert from "node:assert/strict";
import { Readable } from "node:stream";
import { test } from "node:test";
import { importCsv, type Item } from "../inventory.ts";

test("imports the rows of a CSV stream", async () => {
  const inventory = new Map<string, Item>();
  const report = await importCsv(Readable.from(["sku,name,qty\nAB-1,Pen,3\n"]), inventory);
  assert.deepEqual(report, { imported: 1, rejected: [] });
  assert.deepEqual(inventory.get("AB-1"), { sku: "AB-1", name: "Pen", qty: 3 });
});

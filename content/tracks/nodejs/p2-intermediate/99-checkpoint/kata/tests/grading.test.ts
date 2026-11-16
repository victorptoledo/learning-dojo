import assert from "node:assert/strict";
import type { Server } from "node:http";
import type { AddressInfo } from "node:net";
import { Readable } from "node:stream";
import { after, before, describe, test } from "node:test";
import { createInventoryServer, exportNdjson, HttpError, importCsv, parseRow, type Item } from "../inventory.ts";

// fetch types bodies as unknown; any keeps the assertions short.
const json = async (response: Response): Promise<any> => response.json();

describe("parseRow", () => {
  test("accepts a trimmed row", () => {
    assert.deepEqual(parseRow(" AB-12 , Blue pen ,  7 "), { ok: true, item: { sku: "AB-12", name: "Blue pen", qty: 7 } });
    assert.deepEqual(parseRow("XYZ,Zero,0"), { ok: true, item: { sku: "XYZ", name: "Zero", qty: 0 } });
  });

  test("rejects bad rows with a reason", () => {
    for (const line of ["AB-12,pen", "AB-12,pen,1,extra", "ab-12,pen,1", "AB,pen,1", "AB-12,,1", "AB-12,pen,-1", "AB-12,pen,1.5", "AB-12,pen,", "AB-12,pen,many"]) {
      const row = parseRow(line);
      assert.equal(row.ok, false, line);
      assert.ok(!row.ok && row.reason.length > 0);
    }
  });
});

describe("importCsv", () => {
  test("reads lines split across chunks, upserts and reports bad lines by number", async () => {
    const inventory = new Map<string, Item>([["OLD-1", { sku: "OLD-1", name: "Old", qty: 1 }]]);
    const source = Readable.from([Buffer.from("SKU, Name ,QTY\r\nAB-1,Pen,3\r\n\r\nAB-"), Buffer.from("2,Pad,x\r\nOLD-1,Renamed,9\nAB-1,Pen,4")]);
    const report = await importCsv(source, inventory);
    assert.equal(report.imported, 3);
    assert.deepEqual(
      report.rejected.map((each) => each.line),
      [4],
    );
    assert.deepEqual(inventory.get("AB-1"), { sku: "AB-1", name: "Pen", qty: 4 });
    assert.deepEqual(inventory.get("OLD-1"), { sku: "OLD-1", name: "Renamed", qty: 9 });
    assert.equal(inventory.has("AB-2"), false);
  });

  test("400 without the header, or when empty", async () => {
    for (const text of ["AB-1,Pen,3\n", "", "\n\n"]) {
      await assert.rejects(importCsv(Readable.from([text]), new Map()), (error: unknown) => error instanceof HttpError && error.status === 400);
    }
  });
});

describe("exportNdjson", () => {
  test("streams the items sorted by sku, one JSON line each", async () => {
    const inventory = new Map<string, Item>([
      ["B-2", { sku: "B-2", name: "Bee", qty: 2 }],
      ["A-1", { sku: "A-1", name: "Ay", qty: 1 }],
    ]);
    let text = "";
    for await (const chunk of exportNdjson(inventory)) {
      text += String(chunk);
    }
    assert.equal(text, '{"sku":"A-1","name":"Ay","qty":1}\n{"sku":"B-2","name":"Bee","qty":2}\n');
  });
});

describe("the service", () => {
  let server: Server;
  let base = "";
  before(async () => {
    server = createInventoryServer();
    await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
    base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
  });
  after(async () => {
    server.closeAllConnections();
    await new Promise<void>((resolve) => server.close(() => resolve()));
  });

  const upload = (body: RequestInit["body"], type = "text/csv") =>
    fetch(`${base}/import`, { method: "POST", headers: { "content-type": type }, body, duplex: "half" } as RequestInit);

  test("POST /import answers with the report", async () => {
    const response = await upload("sku,name,qty\nAA-1,Apple,5\nbad row\nBB-2,Banana,0\n");
    assert.equal(response.status, 200);
    const report = await json(response);
    assert.equal(report.imported, 2);
    assert.equal(report.rejected.length, 1);
    assert.equal(report.rejected[0].line, 3);
    assert.equal(typeof report.rejected[0].reason, "string");
  });

  test("a large upload streamed in many chunks", async () => {
    const rows = 20_000;
    async function* body(): AsyncGenerator<Uint8Array> {
      yield new TextEncoder().encode("sku,name,qty\n");
      for (let start = 0; start < rows; start += 1000) {
        let chunk = "";
        for (let n = start; n < start + 1000; n += 1) {
          chunk += `BULK-${n},Item ${n},${n % 50}\n`;
        }
        yield new TextEncoder().encode(chunk);
      }
    }
    const response = await upload(Readable.toWeb(Readable.from(body())) as ReadableStream);
    assert.equal(response.status, 200);
    const report = await json(response);
    assert.equal(report.imported, rows);
    assert.deepEqual(report.rejected, []);
  });

  test("GET /items/:sku and GET /items as NDJSON", async () => {
    assert.deepEqual(await json(await fetch(`${base}/items/AA-1`)), { sku: "AA-1", name: "Apple", qty: 5 });
    assert.equal((await fetch(`${base}/items/NOPE-1`)).status, 404);
    const response = await fetch(`${base}/items`);
    assert.equal(response.status, 200);
    assert.equal(response.headers.get("content-type"), "application/x-ndjson");
    const lines = (await response.text()).trim().split("\n");
    assert.equal(lines.length, 20_002);
    assert.deepEqual(JSON.parse(lines[0] ?? ""), { sku: "AA-1", name: "Apple", qty: 5 });
    assert.deepEqual(JSON.parse(lines[1] ?? ""), { sku: "BB-2", name: "Banana", qty: 0 });
  });

  test("415, 400, 405 and 404 as JSON errors", async () => {
    const wrongType = await upload('{"sku":"A"}', "application/json");
    assert.equal(wrongType.status, 415);
    assert.equal(typeof (await json(wrongType)).error, "string");
    const noHeader = await upload("AA-1,Apple,5\n");
    assert.equal(noHeader.status, 400);
    const wrongMethod = await fetch(`${base}/import`);
    assert.equal(wrongMethod.status, 405);
    assert.equal(wrongMethod.headers.get("allow"), "POST");
    assert.equal((await fetch(`${base}/items`, { method: "DELETE" })).status, 405);
    const missing = await fetch(`${base}/nothing`);
    assert.equal(missing.status, 404);
    assert.equal(typeof (await json(missing)).error, "string");
  });

  test("the service still works after the errors", async () => {
    assert.equal((await fetch(`${base}/items/BB-2`)).status, 200);
  });
});

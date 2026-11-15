import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { formatCents, Ledger, parseAmount, parseLine, type Transaction } from "../ledger.ts";

const tx = (
  id: string,
  date: string,
  kind: Transaction["kind"],
  amountCents: number,
  category: string,
): Transaction => ({ id, date, kind, amountCents, category });

describe("parseAmount", () => {
  test("reads whole and decimal amounts as cents", () => {
    assert.equal(parseAmount("12.34"), 1234);
    assert.equal(parseAmount("12"), 1200);
    assert.equal(parseAmount("0.5"), 50);
    assert.equal(parseAmount("0.07"), 7);
    assert.equal(parseAmount("1999.99"), 199999);
  });

  test("rejects what is not a positive amount with at most 2 decimals", () => {
    for (const text of ["", "abc", "-5", "0", "0.00", "1.234", "1,5", "1e3", " ", "1.", ".5"]) {
      assert.equal(parseAmount(text), null, JSON.stringify(text));
    }
  });
});

describe("formatCents", () => {
  test("formats with thousands separators and two decimals", () => {
    assert.equal(formatCents(123456), "1,234.56");
    assert.equal(formatCents(5), "0.05");
    assert.equal(formatCents(0), "0.00");
    assert.equal(formatCents(100000000), "1,000,000.00");
  });

  test("formats negative amounts", () => {
    assert.equal(formatCents(-50), "-0.50");
    assert.equal(formatCents(-123456), "-1,234.56");
  });
});

describe("parseLine", () => {
  test("parses income lines", () => {
    assert.deepEqual(parseLine("t2,2026-01-31,income,3000,salary"), {
      ok: true,
      value: tx("t2", "2026-01-31", "income", 300000, "salary"),
    });
  });

  test("reports the wrong number of fields", () => {
    assert.deepEqual(parseLine("t1,2026-01-01,income,10"), {
      ok: false,
      error: "expected 5 fields, got 4",
    });
  });

  test("reports each invalid field", () => {
    assert.deepEqual(parseLine("t1,2026-02-30,income,10,x"), {
      ok: false,
      error: "invalid date: 2026-02-30",
    });
    assert.deepEqual(parseLine("t1,14/03/2026,income,10,x"), {
      ok: false,
      error: "invalid date: 14/03/2026",
    });
    assert.deepEqual(parseLine("t1,2026-03-14,refund,10,x"), {
      ok: false,
      error: "invalid kind: refund",
    });
    assert.deepEqual(parseLine("t1,2026-03-14,expense,-3,x"), {
      ok: false,
      error: "invalid amount: -3",
    });
    assert.deepEqual(parseLine("t1,2026-03-14,expense,3, "), { ok: false, error: "empty category" });
    assert.deepEqual(parseLine(" ,2026-03-14,expense,3,x"), { ok: false, error: "empty id" });
  });

  test("accepts the 29th of February in a leap year", () => {
    assert.equal(parseLine("t1,2028-02-29,income,1,x").ok, true);
  });
});

describe("Ledger", () => {
  const sample = [
    tx("a", "2026-01-05", "income", 300000, "salary"),
    tx("b", "2026-01-10", "expense", 1250, "food"),
    tx("c", "2026-01-20", "expense", 90000, "rent"),
    tx("d", "2026-02-02", "expense", 3000, "food"),
    tx("e", "2026-02-15", "expense", 90000, "rent"),
    tx("f", "2026-02-28", "income", 5000, "gift"),
  ];

  test("stores transactions and computes the balance", () => {
    const ledger = new Ledger(sample);
    assert.equal(ledger.size, 6);
    assert.equal(ledger.balance(), 305000 - 184250);
    assert.equal(new Ledger().balance(), 0);
  });

  test("rejects a duplicate id", () => {
    const ledger = new Ledger(sample);
    assert.throws(() => ledger.add(tx("a", "2026-03-01", "income", 1, "x")), Error);
    assert.equal(ledger.size, 6);
  });

  test("totals by category, largest first, ties by name", () => {
    const ledger = new Ledger([...sample, tx("g", "2026-03-01", "expense", 4250, "books")]);
    assert.deepEqual(
      [...ledger.totalsByCategory("expense")],
      [
        ["rent", 180000],
        ["books", 4250],
        ["food", 4250],
      ],
    );
    assert.deepEqual([...ledger.totalsByCategory("income")], [
      ["salary", 300000],
      ["gift", 5000],
    ]);
  });

  test("summarizes each month in order", () => {
    const ledger = new Ledger([...sample].reverse());
    assert.deepEqual(ledger.monthly(), [
      { month: "2026-01", income: 300000, expense: 91250, net: 208750 },
      { month: "2026-02", income: 5000, expense: 93000, net: -88000 },
    ]);
    assert.deepEqual(new Ledger().monthly(), []);
  });

  test("fromLines skips comments and blank lines and reports errors with line numbers", () => {
    const { ledger, errors } = Ledger.fromLines([
      "# id,date,kind,amount,category",
      "a,2026-01-05,income,3000,salary",
      "",
      "b,2026-01-10,expense,oops,food",
      "   ",
      "a,2026-01-11,expense,1,food",
      "c,2026-01-12,expense,2.50,food",
    ]);
    assert.equal(ledger.size, 2);
    assert.equal(ledger.balance(), 300000 - 250);
    assert.deepEqual(errors, [
      { line: 4, message: "invalid amount: oops" },
      { line: 6, message: "duplicate id: a" },
    ]);
  });
});

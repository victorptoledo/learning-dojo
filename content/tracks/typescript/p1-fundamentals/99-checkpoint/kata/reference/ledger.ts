// A small typed ledger: parse lines of text, store transactions and build reports.
// Amounts are integer cents, so 12.34 is stored as 1234 and never as a float.

export type Kind = "income" | "expense";

export interface Transaction {
  readonly id: string;
  /** A real calendar date written as YYYY-MM-DD. */
  readonly date: string;
  readonly kind: Kind;
  /** A positive number of cents. */
  readonly amountCents: number;
  readonly category: string;
}

export type Result<T, E = string> = { ok: true; value: T } | { ok: false; error: E };

export interface MonthSummary {
  /** YYYY-MM */
  month: string;
  income: number;
  expense: number;
  net: number;
}

export interface LineError {
  /** 1-based line number. */
  line: number;
  message: string;
}

const AMOUNT = /^(\d+)(?:\.(\d{1,2}))?$/;
const DATE = /^(\d{4})-(\d{2})-(\d{2})$/;
const KINDS: readonly Kind[] = ["income", "expense"];

/** "12.34" -> 1234, "12" -> 1200, "0.5" -> 50; null unless it is a positive amount with at most 2 decimals. */
export function parseAmount(text: string): number | null {
  const match = AMOUNT.exec(text);
  if (!match) {
    return null;
  }
  const [, units = "", decimals = ""] = match;
  const cents = Number(units) * 100 + Number(decimals.padEnd(2, "0"));
  return cents > 0 ? cents : null;
}

/** 123456 -> "1,234.56", -50 -> "-0.50", 0 -> "0.00". */
export function formatCents(cents: number): string {
  const sign = cents < 0 ? "-" : "";
  const absolute = Math.abs(cents);
  const units = Math.floor(absolute / 100)
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  const decimals = (absolute % 100).toString().padStart(2, "0");
  return `${sign}${units}.${decimals}`;
}

function isDate(text: string): boolean {
  const match = DATE.exec(text);
  if (!match) {
    return false;
  }
  const [year, month, day] = match.slice(1).map(Number) as [number, number, number];
  const date = new Date(Date.UTC(year, month - 1, day));
  return (
    date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day
  );
}

function isKind(text: string): text is Kind {
  return (KINDS as readonly string[]).includes(text);
}

/** Parses "id,date,kind,amount,category" (spaces around fields are ignored). */
export function parseLine(line: string): Result<Transaction> {
  const fields = line.split(",").map((field) => field.trim());
  if (fields.length !== 5) {
    return { ok: false, error: `expected 5 fields, got ${fields.length}` };
  }
  const [id, date, kind, amount, category] = fields as [string, string, string, string, string];
  if (id === "") {
    return { ok: false, error: "empty id" };
  }
  if (!isDate(date)) {
    return { ok: false, error: `invalid date: ${date}` };
  }
  if (!isKind(kind)) {
    return { ok: false, error: `invalid kind: ${kind}` };
  }
  const amountCents = parseAmount(amount);
  if (amountCents === null) {
    return { ok: false, error: `invalid amount: ${amount}` };
  }
  if (category === "") {
    return { ok: false, error: "empty category" };
  }
  return { ok: true, value: { id, date, kind, amountCents, category } };
}

export class Ledger {
  readonly #transactions = new Map<string, Transaction>();

  constructor(transactions: Iterable<Transaction> = []) {
    for (const transaction of transactions) {
      this.add(transaction);
    }
  }

  /** Reads lines, skipping blank lines and lines starting with #, and collects the errors. */
  static fromLines(lines: Iterable<string>): { ledger: Ledger; errors: LineError[] } {
    const ledger = new Ledger();
    const errors: LineError[] = [];
    let line = 0;
    for (const text of lines) {
      line += 1;
      const trimmed = text.trim();
      if (trimmed === "" || trimmed.startsWith("#")) {
        continue;
      }
      const parsed = parseLine(trimmed);
      if (!parsed.ok) {
        errors.push({ line, message: parsed.error });
      } else if (ledger.#transactions.has(parsed.value.id)) {
        errors.push({ line, message: `duplicate id: ${parsed.value.id}` });
      } else {
        ledger.add(parsed.value);
      }
    }
    return { ledger, errors };
  }

  /** Adds a transaction; an Error when its id is already in the ledger. */
  add(transaction: Transaction): void {
    if (this.#transactions.has(transaction.id)) {
      throw new Error(`duplicate id: ${transaction.id}`);
    }
    this.#transactions.set(transaction.id, transaction);
  }

  /** The number of transactions. */
  get size(): number {
    return this.#transactions.size;
  }

  /** Income minus expenses, in cents. */
  balance(): number {
    let total = 0;
    for (const { kind, amountCents } of this.#transactions.values()) {
      total += kind === "income" ? amountCents : -amountCents;
    }
    return total;
  }

  /** Total cents per category for one kind, the largest first, ties by category name. */
  totalsByCategory(kind: Kind): Map<string, number> {
    const totals = new Map<string, number>();
    for (const transaction of this.#transactions.values()) {
      if (transaction.kind === kind) {
        totals.set(
          transaction.category,
          (totals.get(transaction.category) ?? 0) + transaction.amountCents,
        );
      }
    }
    const sorted = [...totals].sort(([nameA, a], [nameB, b]) => b - a || nameA.localeCompare(nameB));
    return new Map(sorted);
  }

  /** One summary per month that has transactions, in month order. */
  monthly(): MonthSummary[] {
    const months = new Map<string, MonthSummary>();
    for (const { date, kind, amountCents } of this.#transactions.values()) {
      const month = date.slice(0, 7);
      const summary = months.get(month) ?? { month, income: 0, expense: 0, net: 0 };
      summary[kind] += amountCents;
      summary.net = summary.income - summary.expense;
      months.set(month, summary);
    }
    return [...months.values()].sort((a, b) => a.month.localeCompare(b.month));
  }
}

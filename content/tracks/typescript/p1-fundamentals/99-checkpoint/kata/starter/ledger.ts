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

/** "12.34" -> 1234, "12" -> 1200, "0.5" -> 50; null unless it is a positive amount with at most 2 decimals. */
export function parseAmount(text: string): number | null {
  throw new Error("todo: write parseAmount()");
}

/** 123456 -> "1,234.56", -50 -> "-0.50", 0 -> "0.00". */
export function formatCents(cents: number): string {
  throw new Error("todo: write formatCents()");
}

/** Parses "id,date,kind,amount,category" (spaces around fields are ignored). */
export function parseLine(line: string): Result<Transaction> {
  throw new Error("todo: write parseLine()");
}

export class Ledger {
  constructor(transactions: Iterable<Transaction> = []) {
    throw new Error("todo: write the Ledger constructor");
  }

  /** Reads lines, skipping blank lines and lines starting with #, and collects the errors. */
  static fromLines(lines: Iterable<string>): { ledger: Ledger; errors: LineError[] } {
    throw new Error("todo: write Ledger.fromLines()");
  }

  /** Adds a transaction; an Error when its id is already in the ledger. */
  add(transaction: Transaction): void {
    throw new Error("todo: write add()");
  }

  /** The number of transactions. */
  get size(): number {
    throw new Error("todo: write size");
  }

  /** Income minus expenses, in cents. */
  balance(): number {
    throw new Error("todo: write balance()");
  }

  /** Total cents per category for one kind, the largest first, ties by category name. */
  totalsByCategory(kind: Kind): Map<string, number> {
    throw new Error("todo: write totalsByCategory()");
  }

  /** One summary per month that has transactions, in month order. */
  monthly(): MonthSummary[] {
    throw new Error("todo: write monthly()");
  }
}

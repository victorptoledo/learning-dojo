// `tally sum <file> [--month YYYY-MM]`: the totals of a ledger file, per currency.
import { formatMoney, parseMoney, sum, type Currency, type Money } from "@tally/core";

export type Io = { readFile(path: string): string; out(line: string): void; err(line: string): void };

const USAGE = "usage: tally sum <file> [--month YYYY-MM]";
const CURRENCY_ORDER: readonly Currency[] = ["BRL", "USD", "EUR"];
const DATE = /^\d{4}-\d{2}-\d{2}$/;
const MONTH = /^\d{4}-\d{2}$/;

type Expense = { date: string; amount: Money; description: string };
type Line = { ok: true; expense: Expense | null } | { ok: false; reason: string };

function parseLine(text: string): Line {
  const trimmed = text.trim();
  if (trimmed === "" || trimmed.startsWith("#")) {
    return { ok: true, expense: null };
  }
  const [date = "", amount = "", currency = "", ...words] = trimmed.split(/\s+/);
  if (!DATE.test(date)) {
    return { ok: false, reason: `invalid date: ${date}` };
  }
  const parsed = parseMoney(`${amount} ${currency}`);
  if (!parsed.ok) {
    return { ok: false, reason: parsed.error };
  }
  if (words.length === 0) {
    return { ok: false, reason: "missing description" };
  }
  return { ok: true, expense: { date, amount: parsed.value, description: words.join(" ") } };
}

function parseArgs(argv: readonly string[]): { file: string; month: string | undefined } | undefined {
  const [command, file, ...rest] = argv;
  if (command !== "sum" || file === undefined || file.startsWith("--")) {
    return undefined;
  }
  if (rest.length === 0) {
    return { file, month: undefined };
  }
  const [option, month] = rest;
  if (rest.length === 2 && option === "--month" && month !== undefined && MONTH.test(month)) {
    return { file, month };
  }
  return undefined;
}

export function run(argv: readonly string[], io: Io): number {
  const args = parseArgs(argv);
  if (args === undefined) {
    io.err(USAGE);
    return 2;
  }
  let text: string;
  try {
    text = io.readFile(args.file);
  } catch {
    io.err(`error: cannot read ${args.file}`);
    return 1;
  }
  const expenses: Expense[] = [];
  const lines = text.split(/\r?\n/);
  for (const [index, line] of lines.entries()) {
    const parsed = parseLine(line);
    if (!parsed.ok) {
      io.err(`line ${index + 1}: ${parsed.reason}`);
      return 1;
    }
    if (parsed.expense !== null && (args.month === undefined || parsed.expense.date.startsWith(`${args.month}-`))) {
      expenses.push(parsed.expense);
    }
  }
  if (expenses.length === 0) {
    io.out("no expenses");
    return 0;
  }
  for (const currency of CURRENCY_ORDER) {
    const amounts = expenses.filter((expense) => expense.amount.currency === currency).map((expense) => expense.amount);
    if (amounts.length > 0) {
      io.out(formatMoney(sum(amounts, currency)));
    }
  }
  return 0;
}

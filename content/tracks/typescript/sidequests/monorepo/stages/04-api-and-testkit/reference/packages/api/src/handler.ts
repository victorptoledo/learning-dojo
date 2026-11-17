// An HTTP-shaped handler over an in-memory list of expenses.
import { formatMoney, parseMoney, sum, type Currency, type Money } from "@tally/core";

export type ApiRequest = { method: string; path: string; body?: unknown };
export type ApiResponse = { status: number; body: unknown };

type Expense = { id: string; date: string; amount: Money; description: string };

const DATE = /^\d{4}-\d{2}-\d{2}$/;
const CURRENCY_ORDER: readonly Currency[] = ["BRL", "USD", "EUR"];

const error = (status: number, message: string): ApiResponse => ({ status, body: { error: message } });

const view = (expense: Expense) => ({
  id: expense.id,
  date: expense.date,
  amount: formatMoney(expense.amount),
  description: expense.description,
});

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

type Parsed = { ok: true; date: string; amount: Money; description: string } | { ok: false; error: string };

function parseExpense(body: unknown): Parsed {
  if (!isRecord(body)) {
    return { ok: false, error: "body must be an object" };
  }
  const { date, amount, description } = body;
  if (typeof date !== "string" || !DATE.test(date)) {
    return { ok: false, error: `invalid date: ${String(date)}` };
  }
  if (typeof amount !== "string") {
    return { ok: false, error: "amount must be text" };
  }
  const money = parseMoney(amount);
  if (!money.ok) {
    return { ok: false, error: money.error };
  }
  if (typeof description !== "string" || description.trim() === "") {
    return { ok: false, error: "missing description" };
  }
  return { ok: true, date, amount: money.value, description: description.trim() };
}

export function createHandler(): (request: ApiRequest) => ApiResponse {
  const expenses: Expense[] = [];
  let lastId = 0;

  const find = (id: string) => expenses.findIndex((expense) => expense.id === id);

  return (request) => {
    const { method, path } = request;
    if (path === "/expenses") {
      if (method === "GET") {
        return { status: 200, body: expenses.map(view) };
      }
      if (method === "POST") {
        const parsed = parseExpense(request.body);
        if (!parsed.ok) {
          return error(400, parsed.error);
        }
        lastId += 1;
        const expense: Expense = { id: String(lastId), date: parsed.date, amount: parsed.amount, description: parsed.description };
        expenses.push(expense);
        return { status: 201, body: view(expense) };
      }
      return error(405, "method not allowed");
    }
    const match = /^\/expenses\/([^/]+)$/.exec(path);
    if (match !== null) {
      const id = match[1] ?? "";
      if (method !== "GET" && method !== "DELETE") {
        return error(405, "method not allowed");
      }
      const index = find(id);
      const expense = expenses[index];
      if (expense === undefined) {
        return error(404, `no expense ${id}`);
      }
      if (method === "DELETE") {
        expenses.splice(index, 1);
        return { status: 204, body: null };
      }
      return { status: 200, body: view(expense) };
    }
    if (path === "/totals") {
      if (method !== "GET") {
        return error(405, "method not allowed");
      }
      const totals: Record<string, string> = {};
      for (const currency of CURRENCY_ORDER) {
        const amounts = expenses.filter((expense) => expense.amount.currency === currency).map((expense) => expense.amount);
        if (amounts.length > 0) {
          totals[currency] = formatMoney(sum(amounts, currency));
        }
      }
      return { status: 200, body: totals };
    }
    return error(404, "not found");
  };
}

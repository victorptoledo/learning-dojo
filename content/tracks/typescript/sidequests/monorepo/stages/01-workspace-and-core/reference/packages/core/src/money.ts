// Money in integer cents. The brand makes `money()` and `parseMoney()` the only ways to get
// one, so a `Money` is always a safe integer of a known currency.

export type Currency = "BRL" | "USD" | "EUR";

declare const brand: unique symbol;

export type Money = { readonly cents: number; readonly currency: Currency; readonly [brand]: true };

export type Parsed = { ok: true; value: Money } | { ok: false; error: string };

const CURRENCIES: readonly string[] = ["BRL", "USD", "EUR"] satisfies Currency[];
const AMOUNT = /^(-?)(\d+)(?:\.(\d{1,2}))?$/;

export function isCurrency(value: unknown): value is Currency {
  return typeof value === "string" && CURRENCIES.includes(value);
}

export function money(cents: number, currency: Currency): Money {
  if (!Number.isSafeInteger(cents)) {
    throw new RangeError("cents must be a safe integer");
  }
  return { cents: cents === 0 ? 0 : cents, currency } as Money;
}

export function parseMoney(text: string): Parsed {
  const parts = text.trim().split(/\s+/);
  if (parts.length !== 2) {
    return { ok: false, error: `invalid money: ${text}` };
  }
  const [amount = "", currency = ""] = parts;
  if (!isCurrency(currency)) {
    return { ok: false, error: `unknown currency: ${currency}` };
  }
  const match = AMOUNT.exec(amount);
  if (match === null) {
    return { ok: false, error: `invalid amount: ${amount}` };
  }
  const [, sign, units = "", fraction = ""] = match;
  const cents = Number(units) * 100 + Number(fraction.padEnd(2, "0"));
  if (!Number.isSafeInteger(cents)) {
    return { ok: false, error: `invalid amount: ${amount}` };
  }
  return { ok: true, value: money(sign === "-" ? -cents : cents, currency) };
}

export function formatMoney(value: Money): string {
  const absolute = Math.abs(value.cents);
  const units = Math.floor(absolute / 100);
  const fraction = String(absolute % 100).padStart(2, "0");
  return `${value.cents < 0 ? "-" : ""}${units}.${fraction} ${value.currency}`;
}

export function add(a: Money, b: Money): Money {
  if (a.currency !== b.currency) {
    throw new TypeError(`cannot add ${a.currency} and ${b.currency}`);
  }
  return money(a.cents + b.cents, a.currency);
}

export function sum(values: readonly Money[], currency: Currency): Money {
  return values.reduce((total, value) => add(total, value), money(0, currency));
}

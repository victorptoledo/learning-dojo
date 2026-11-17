import { isCurrency, money, parseMoney, type Currency, type Money, type Parsed } from "@tally/core";
import type { Equal, Expect } from "./expect.ts";

// test: Currency is the three codes
export type Codes = Expect<Equal<Currency, "BRL" | "USD" | "EUR">>;

// test: Money is branded: an object literal is not a Money
// @ts-expect-error: only money() and parseMoney() make a Money
export const fake: Money = { cents: 100, currency: "BRL" };

// test: Money is read-only
export const real = money(1, "BRL");
// @ts-expect-error: cents is read-only
real.cents = 2;

// test: Money has typed cents and currency
export const centsOf = (value: Money): number => value.cents;
export const currencyOf = (value: Money): Currency => value.currency;

// test: money only takes known currencies
// @ts-expect-error: "GBP" is not a Currency
money(1, "GBP");

// test: parseMoney returns Parsed, narrowed by ok
const parsed = parseMoney("1 BRL");
export type ParsedType = Expect<Equal<typeof parsed, Parsed>>;
export const parsedValue: Money | undefined = parsed.ok ? parsed.value : undefined;
export const parsedError: string | undefined = parsed.ok ? undefined : parsed.error;

// test: isCurrency narrows unknown values
export function currencyFrom(value: unknown): Currency | undefined {
  return isCurrency(value) ? value : undefined;
}

// test: the internals of @tally/core are not reachable
// @ts-expect-error: only the exports of @tally/core resolve
import("@tally/core/src/index.ts");
// @ts-expect-error: only the exports of @tally/core resolve
import("@tally/core/package.json");

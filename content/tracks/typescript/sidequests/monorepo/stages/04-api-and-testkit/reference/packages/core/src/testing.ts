// @tally/core/testing: helpers for tests only. Production code imports "@tally/core".
import { parseMoney, type Money } from "./money.ts";

/** Parses like parseMoney, but throws on bad input: `moneyOf("12.50 BRL")`. */
export function moneyOf(text: string): Money {
  const parsed = parseMoney(text);
  if (!parsed.ok) {
    throw new Error(parsed.error);
  }
  return parsed.value;
}

// The cart page on release day: a live total in the visitor's language, errors reported with the
// release that caused them, and a payment script that may never arrive. See README.md.
import "./quantity-input.js";

export const ERRORS_ENDPOINT = "/api/errors";
export const PAYMENT_SCRIPT = "/vendor/pay.js";
export const PAYMENT_TIMEOUT_MS = 3000;

/** A price in cents as currency text for the locale, such as "€29.00" for en-IE and EUR. */
export function formatPrice(cents, currency, locale) {
  throw new Error("todo: write formatPrice()");
}

/** "1 item", "3 items", "1,000 items" for English; "1 item", "2 itens" for Portuguese. */
export function itemsLabel(count, locale) {
  throw new Error("todo: write itemsLabel()");
}

/** { count, cents } of the cart form: the quantities and the sum of price × quantity. */
export function cartSummary(form) {
  throw new Error("todo: write cartSummary()");
}

/** Writes the summary of the form into #count and #total. */
export function updateSummary(form) {
  throw new Error("todo: write updateSummary()");
}

/** Sends one problem to ERRORS_ENDPOINT; false when it was not sent (repeated or over the cap). */
export function reportProblem(kind, message) {
  throw new Error("todo: write reportProblem()");
}

/** Reports uncaught errors and unhandled promise rejections. */
export function installErrorReporting() {
  throw new Error("todo: write installErrorReporting()");
}

/** Adds a <script> and resolves when it loads; rejects when it fails or takes too long. */
export function loadScript(src, timeoutMs) {
  throw new Error("todo: write loadScript()");
}

/** Loads the payment provider's script and mounts it, or falls back to bank transfer. */
export async function setupPayments() {
  throw new Error("todo: write setupPayments()");
}

// todo: install the error reporting first, then hide the Update cart button, keep the summary
// up to date on "quantity-change", and set up the payments

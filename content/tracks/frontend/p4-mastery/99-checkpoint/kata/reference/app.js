// The cart page on release day: a live total in the visitor's language, errors reported with the
// release that caused them, and a payment script that may never arrive. See README.md.
import "./quantity-input.js";

export const ERRORS_ENDPOINT = "/api/errors";
export const PAYMENT_SCRIPT = "/vendor/pay.js";
export const PAYMENT_TIMEOUT_MS = 3000;
const MAX_REPORTS = 5;

const ITEM_WORDS = {
  en: { one: "item", other: "items" },
  pt: { one: "item", other: "itens" },
};

const RELEASE = document.querySelector('meta[name="release"]')?.content ?? "unknown";
const reported = new Set();

/** A price in cents as currency text for the locale, such as "€29.00" for en-IE and EUR. */
export function formatPrice(cents, currency, locale) {
  return new Intl.NumberFormat(locale, { style: "currency", currency }).format(cents / 100);
}

/** "1 item", "3 items", "1,000 items" for English; "1 item", "2 itens" for Portuguese. */
export function itemsLabel(count, locale) {
  const words = ITEM_WORDS[new Intl.Locale(locale).language] ?? ITEM_WORDS.en;
  const category = new Intl.PluralRules(locale).select(count);
  return `${new Intl.NumberFormat(locale).format(count)} ${words[category] ?? words.other}`;
}

/** { count, cents } of the cart form: the quantities and the sum of price × quantity. */
export function cartSummary(form) {
  let count = 0;
  let cents = 0;
  for (const item of form.querySelectorAll(".item")) {
    const quantity = Number(item.querySelector('input[type="number"]')?.value) || 0;
    count += quantity;
    cents += quantity * Number(item.dataset.price);
  }
  return { count, cents };
}

/** Writes the summary of the form into #count and #total. */
export function updateSummary(form) {
  const locale = document.documentElement.lang || "en";
  const { count, cents } = cartSummary(form);
  form.querySelector("#count").textContent = itemsLabel(count, locale);
  form.querySelector("#total").textContent = formatPrice(cents, form.dataset.currency, locale);
}

/** Sends one problem to ERRORS_ENDPOINT; false when it was not sent (repeated or over the cap). */
export function reportProblem(kind, message) {
  const text = String(message);
  if (reported.has(text) || reported.size >= MAX_REPORTS) {
    return false;
  }
  reported.add(text);
  const body = JSON.stringify({ kind, message: text, release: RELEASE, page: location.pathname });
  // sendBeacon survives the page being closed; a blob sets the content type.
  return navigator.sendBeacon(ERRORS_ENDPOINT, new Blob([body], { type: "application/json" }));
}

/** Reports uncaught errors and unhandled promise rejections. */
export function installErrorReporting() {
  window.addEventListener("error", (event) => {
    reportProblem("error", event.error?.message ?? event.message);
  });
  window.addEventListener("unhandledrejection", (event) => {
    reportProblem("rejection", event.reason?.message ?? String(event.reason));
  });
}

/** Adds a <script> and resolves when it loads; rejects when it fails or takes too long. */
export function loadScript(src, timeoutMs) {
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    const timer = setTimeout(() => {
      script.remove();
      reject(new Error(`${src} took longer than ${timeoutMs} ms`));
    }, timeoutMs);
    script.addEventListener("load", () => {
      clearTimeout(timer);
      resolve();
    });
    script.addEventListener("error", () => {
      clearTimeout(timer);
      script.remove();
      reject(new Error(`${src} failed to load`));
    });
    script.async = true;
    script.src = src;
    document.head.append(script);
  });
}

/** Loads the payment provider's script and mounts it, or falls back to bank transfer. */
export async function setupPayments() {
  const payment = document.querySelector("#payment");
  const status = document.querySelector("#payment-status");
  const bank = document.querySelector("#bank-transfer");
  status.textContent = "Loading card payments…";
  bank.hidden = true;
  try {
    await loadScript(PAYMENT_SCRIPT, PAYMENT_TIMEOUT_MS);
    if (typeof window.Pay?.mount !== "function") {
      throw new Error(`${PAYMENT_SCRIPT} loaded without Pay.mount`);
    }
    window.Pay.mount(payment);
    status.textContent = "";
  } catch (error) {
    status.textContent = "Card payments are unavailable right now.";
    bank.hidden = false;
    reportProblem("payment", error.message);
  }
}

installErrorReporting();
const cart = document.querySelector("#cart");
cart.querySelector("#update").hidden = true;
cart.addEventListener("quantity-change", () => updateSummary(cart));
updateSummary(cart);
setupPayments();

// Loaded with --import by `npm test`: a jsdom document as the global DOM, so React renders
// into it and Testing Library queries it, and cleanup() after each test. The dojo uses its own copy.
import { JSDOM } from "jsdom";
import { afterEach } from "node:test";

const dom = new JSDOM("<!doctype html><html><head></head><body></body></html>", {
  url: "http://localhost/",
  pretendToBeVisual: true,
});
const { window } = dom;

for (const key of Object.getOwnPropertyNames(window)) {
  if (key in globalThis) {
    continue;
  }
  Object.defineProperty(globalThis, key, {
    configurable: true,
    get: () => window[key],
  });
}
for (const key of ["window", "document", "navigator", "self"]) {
  Object.defineProperty(globalThis, key, {
    configurable: true,
    writable: true,
    value: key === "self" ? window : key === "window" ? window : window[key],
  });
}

// Tells React that tests wrap updates in act(), as Testing Library does.
globalThis.IS_REACT_ACT_ENVIRONMENT = true;

// Testing Library only cleans up by itself when a global afterEach exists (Jest, Vitest).
const { cleanup } = await import("@testing-library/react");
afterEach(() => {
  cleanup();
});

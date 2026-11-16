// Search as you type: wait until the user pauses, cancel the request that is no longer wanted,
// remember answers, and keep the query in the URL and in localStorage. See README.md.

const input = document.querySelector("#q");
const results = document.querySelector("#results");
const status = document.querySelector("#status");

export const DELAY_MS = 300;
export const STORAGE_KEY = "last-query";

/** A function that calls fn(...args) once, DELAY ms after the last call. */
export function debounce(fn, ms) {
  throw new Error("todo: write debounce()");
}

/** "/api/search?q=..." with the query encoded by URLSearchParams. */
export function searchUrl(query) {
  throw new Error("todo: write searchUrl()");
}

/** Fetches the books for a query: [{ id, title, author }]. Throws on an HTTP error status. */
export async function search(query, { signal } = {}) {
  throw new Error("todo: write search()");
}

/** "No results", "1 result", "2 results". */
export function resultLabel(n) {
  throw new Error("todo: write resultLabel()");
}

/** Runs one search for the (trimmed) query and shows the outcome. */
export async function runSearch(query) {
  throw new Error("todo: write runSearch()");
}

// todo: run a debounced search on every input event
// todo: on load, search for ?q= from the URL, or else for the query saved in localStorage

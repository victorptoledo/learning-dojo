// Search as you type: wait until the user pauses, cancel the request that is no longer wanted,
// remember answers, and keep the query in the URL and in localStorage. See README.md.

const input = document.querySelector("#q");
const results = document.querySelector("#results");
const status = document.querySelector("#status");

export const DELAY_MS = 300;
export const STORAGE_KEY = "last-query";

/** A function that calls fn(...args) once, DELAY ms after the last call. */
export function debounce(fn, ms) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  };
}

/** "/api/search?q=..." with the query encoded by URLSearchParams. */
export function searchUrl(query) {
  return `/api/search?${new URLSearchParams({ q: query })}`;
}

/** Fetches the books for a query: [{ id, title, author }]. Throws on an HTTP error status. */
export async function search(query, { signal } = {}) {
  const response = await fetch(searchUrl(query), { signal, headers: { accept: "application/json" } });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}

/** "No results", "1 result", "2 results". */
export function resultLabel(n) {
  if (n === 0) {
    return "No results";
  }
  return `${n} ${n === 1 ? "result" : "results"}`;
}

const cache = new Map();
let controller = null;

function show(books) {
  results.replaceChildren(
    ...books.map((book) => {
      const item = document.createElement("li");
      item.textContent = `${book.title} — ${book.author}`;
      return item;
    }),
  );
  status.textContent = resultLabel(books.length);
}

function remember(query) {
  const url = new URL(location.href);
  if (query) {
    url.searchParams.set("q", query);
  } else {
    url.searchParams.delete("q");
  }
  history.replaceState(null, "", url);
  try {
    localStorage.setItem(STORAGE_KEY, query);
  } catch {
    // Storage can be full or disabled: the search still works.
  }
}

/** Runs one search for the (trimmed) query and shows the outcome. */
export async function runSearch(query) {
  query = query.trim();
  controller?.abort();
  controller = null;
  results.setAttribute("aria-busy", "false");
  remember(query);
  if (query === "") {
    results.replaceChildren();
    status.textContent = "";
    return;
  }
  if (cache.has(query)) {
    show(cache.get(query));
    return;
  }
  const mine = new AbortController();
  controller = mine;
  results.setAttribute("aria-busy", "true");
  status.textContent = "Searching…";
  try {
    const books = await search(query, { signal: mine.signal });
    cache.set(query, books);
    show(books);
  } catch (error) {
    if (error.name === "AbortError") {
      return;
    }
    results.replaceChildren();
    status.textContent = "Search failed. Try again.";
  } finally {
    if (controller === mine) {
      controller = null;
      results.setAttribute("aria-busy", "false");
    }
  }
}

const searchLater = debounce(runSearch, DELAY_MS);
input.addEventListener("input", () => searchLater(input.value));

const initial = new URLSearchParams(location.search).get("q") ?? localStorage.getItem(STORAGE_KEY) ?? "";
if (initial.trim() !== "") {
  input.value = initial;
  runSearch(initial);
}

// Runs once the page is parsed: type="module" scripts are deferred.

/** The greeting for a name: "Hello from Ada Lovelace!". Blank names give "Hello from the dojo!". */
export function greeting(name) {
  throw new Error("todo: write greeting()");
}

const button = document.querySelector("#say-hello");
const output = document.querySelector("#greeting");
const name = document.querySelector("h1");

// todo: when the button is clicked, show greeting(<the h1's text>) in the output.

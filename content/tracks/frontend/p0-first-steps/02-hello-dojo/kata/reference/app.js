// Runs once the page is parsed: type="module" scripts are deferred.

/** The greeting for a name: "Hello from Ada Lovelace!". Blank names give "Hello from the dojo!". */
export function greeting(name) {
  const who = name?.trim() || "the dojo";
  return `Hello from ${who}!`;
}

const button = document.querySelector("#say-hello");
const output = document.querySelector("#greeting");
const name = document.querySelector("h1");

button.addEventListener("click", () => {
  output.textContent = greeting(name.textContent);
});

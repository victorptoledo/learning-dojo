// Shows the comments of a page. Every field of a comment is written by a stranger:
// treat it as text, and only link to http(s) addresses. See README.md.

const list = document.querySelector("#comments");
const form = document.querySelector("#new-comment");

/** The URL as a string when it is an absolute http: or https: URL, otherwise null. */
export function safeUrl(value) {
  throw new Error("todo: write safeUrl()");
}

/** The <li class="comment"> for { author, website, body }. */
export function commentElement(comment) {
  // todo: this builds HTML out of strangers' text: an XSS hole. Use createElement and textContent.
  const item = document.createElement("li");
  item.className = "comment";
  const name = comment.website
    ? `<a href="${comment.website}">${comment.author}</a>`
    : comment.author;
  item.innerHTML = `<p class="author">${name}</p><p class="body">${comment.body}</p>`;
  return item;
}

/** Fetches the comments and shows them. */
export async function loadComments() {
  const response = await fetch(window.COMMENTS_URL);
  const comments = await response.json();
  list.replaceChildren(...comments.map(commentElement));
}

window.loadComments = loadComments;

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const data = new FormData(form);
  list.append(
    commentElement({
      author: String(data.get("author")),
      website: String(data.get("website") ?? ""),
      body: String(data.get("body")),
    }),
  );
  form.reset();
});

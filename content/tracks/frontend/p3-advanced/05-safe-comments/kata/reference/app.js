// Shows the comments of a page. Every field of a comment is written by a stranger:
// treat it as text, and only link to http(s) addresses. See README.md.

const COMMENTS_URL = "/api/comments";
const list = document.querySelector("#comments");
const form = document.querySelector("#new-comment");

/** The URL as a string when it is an absolute http: or https: URL, otherwise null. */
export function safeUrl(value) {
  if (typeof value !== "string" || value.trim() === "") {
    return null;
  }
  let url;
  try {
    url = new URL(value.trim());
  } catch {
    return null;
  }
  return url.protocol === "http:" || url.protocol === "https:" ? url.href : null;
}

/** The <li class="comment"> for { author, website, body }. */
export function commentElement(comment) {
  const item = document.createElement("li");
  item.className = "comment";

  const author = document.createElement("p");
  author.className = "author";
  const href = safeUrl(comment.website);
  if (href) {
    const link = document.createElement("a");
    link.href = href;
    link.rel = "nofollow ugc noopener noreferrer";
    link.textContent = comment.author;
    author.append(link);
  } else {
    author.textContent = comment.author;
  }

  const body = document.createElement("p");
  body.className = "body";
  body.textContent = comment.body;

  item.append(author, body);
  return item;
}

/** Fetches the comments and shows them. */
export async function loadComments() {
  const response = await fetch(COMMENTS_URL);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  const comments = await response.json();
  list.replaceChildren(...comments.map(commentElement));
}

document.querySelector("#load").addEventListener("click", () => {
  loadComments();
});

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const data = new FormData(form);
  list.append(
    commentElement({
      author: String(data.get("author") ?? ""),
      website: String(data.get("website") ?? ""),
      body: String(data.get("body") ?? ""),
    }),
  );
  form.reset();
});

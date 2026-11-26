// The product page of the Dojo Tee: reviews from strangers, rendered safely, and a size guide
// that is only downloaded when someone opens it. See README.md.

const reviewsList = document.querySelector("#reviews");
const reviewsStatus = document.querySelector("#reviews-status");
const guideButton = document.querySelector("#size-guide-button");
const guide = document.querySelector("#size-guide");

/** The URL as a string when it is an absolute http(s) URL, otherwise null. */
export function safeUrl(value) {
  if (typeof value !== "string") {
    return null;
  }
  try {
    const url = new URL(value.trim());
    return url.protocol === "http:" || url.protocol === "https:" ? url.href : null;
  } catch {
    return null;
  }
}

function paragraph(className, text) {
  const p = document.createElement("p");
  p.className = className;
  p.textContent = text;
  return p;
}

/** One review as <li class="review">, built from text only (see README.md). */
export function reviewElement(review) {
  const li = document.createElement("li");
  li.className = "review";
  const author = paragraph("review-author", "");
  const href = safeUrl(review.website);
  if (href) {
    const link = document.createElement("a");
    link.href = href;
    link.rel = "nofollow ugc noopener noreferrer";
    link.textContent = String(review.author ?? "");
    author.append(link);
  } else {
    author.textContent = String(review.author ?? "");
  }
  li.append(
    author,
    paragraph("review-rating", `Rated ${Number(review.rating)} out of 5`),
    paragraph("review-text", String(review.text ?? "")),
  );
  return li;
}

/** Fetches /api/reviews and shows them, or a message when they cannot be loaded. */
export async function loadReviews() {
  reviewsStatus.textContent = "Loading reviews…";
  try {
    const response = await fetch("/api/reviews");
    if (!response.ok) {
      throw new Error(`GET /api/reviews answered ${response.status}`);
    }
    const reviews = await response.json();
    // One DOM write for the whole list.
    const items = document.createDocumentFragment();
    for (const review of reviews) {
      items.append(reviewElement(review));
    }
    reviewsList.replaceChildren(items);
    reviewsStatus.textContent = reviews.length === 1 ? "1 review" : `${reviews.length} reviews`;
  } catch {
    reviewsStatus.textContent = "Reviews are unavailable right now.";
  }
}

/** Opens or closes the size guide, rendering it the first time. */
export async function openSizeGuide() {
  const open = guideButton.getAttribute("aria-expanded") === "true";
  if (open) {
    guide.hidden = true;
    guideButton.setAttribute("aria-expanded", "false");
    return;
  }
  if (!guide.hasChildNodes()) {
    // A separate chunk: only visitors who open the guide download it.
    const { renderSizeGuide } = await import("./size-guide.js");
    renderSizeGuide(guide);
  }
  guide.hidden = false;
  guideButton.setAttribute("aria-expanded", "true");
}

document.documentElement.classList.add("js");
guideButton.addEventListener("click", openSizeGuide);
loadReviews();

// The product page of the Dojo Tee: reviews from strangers, rendered safely, and a size guide
// that is only downloaded when someone opens it. See README.md.

// todo: this static import puts the size guide in every visitor's download
import { renderSizeGuide } from "./size-guide.js";

const reviewsList = document.querySelector("#reviews");
const reviewsStatus = document.querySelector("#reviews-status");
const guideButton = document.querySelector("#size-guide-button");
const guide = document.querySelector("#size-guide");

/** The URL as a string when it is an absolute http(s) URL, otherwise null. */
export function safeUrl(value) {
  throw new Error("todo: write safeUrl()");
}

/** One review as <li class="review">, built from text only (see README.md). */
export function reviewElement(review) {
  // The old version, kept for reference: it trusts strangers' text.
  const li = document.createElement("li");
  li.className = "review";
  li.innerHTML = `<p class="review-author"><a href="${review.website}">${review.author}</a></p>
    <p class="review-rating">Rated ${review.rating} out of 5</p>
    <p class="review-text">${review.text}</p>`;
  return li;
}

/** Fetches /api/reviews and shows them, or a message when they cannot be loaded. */
export async function loadReviews() {
  throw new Error("todo: write loadReviews()");
}

/** Opens or closes the size guide, rendering it the first time. */
export async function openSizeGuide() {
  throw new Error("todo: write openSizeGuide()");
}

// todo: add the "js" class to <html> here, listen for clicks on the size guide button, and
// load the reviews
loadReviews();

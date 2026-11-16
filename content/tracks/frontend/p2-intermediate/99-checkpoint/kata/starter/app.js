// A feedback form that validates with the Constraint Validation API, keeps a draft in
// localStorage so nothing typed is lost, and sends JSON with fetch. See README.md.

export const DRAFT_KEY = "feedback-draft";
export const FIELDS = ["name", "email", "rating", "message"];

const form = document.querySelector("#feedback");
const status = document.querySelector("#form-status");
const counter = document.querySelector("#message-count");
const submit = form.querySelector('button[type="submit"]');

/** The message for one field (for the rating, any of its radios), or "" when it is valid. */
export function errorFor(field) {
  throw new Error("todo: write errorFor()");
}

/** Shows or clears the error of one field (by name) and sets aria-invalid; true when valid. */
export function checkField(form, name) {
  throw new Error("todo: write checkField()");
}

/** Checks every field; returns the first invalid control (to focus), or null. */
export function validate(form) {
  throw new Error("todo: write validate()");
}

/** The form's values as { name, email, rating, message } (strings; rating "" if none). */
export function formValues(form) {
  throw new Error("todo: write formValues()");
}

/** Saves formValues(form) as JSON in localStorage under DRAFT_KEY. */
export function saveDraft(form) {
  throw new Error("todo: write saveDraft()");
}

/** Fills the form from the saved draft, if there is one (and ignores a broken one). */
export function loadDraft(form) {
  throw new Error("todo: write loadDraft()");
}

/** POSTs the values as JSON to /api/feedback; returns the response JSON, throws if not ok. */
export async function sendFeedback(values) {
  throw new Error("todo: write sendFeedback()");
}

// todo: on load, restore the draft and update the counter
// todo: on input, save the draft, update the counter, and re-check a field already marked invalid
// todo: on submit, validate, then send (see README.md for the messages and the button)

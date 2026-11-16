// A feedback form that validates with the Constraint Validation API, keeps a draft in
// localStorage so nothing typed is lost, and sends JSON with fetch. See README.md.

export const DRAFT_KEY = "feedback-draft";
export const FIELDS = ["name", "email", "rating", "message"];

const form = document.querySelector("#feedback");
const status = document.querySelector("#form-status");
const counter = document.querySelector("#message-count");
const submit = form.querySelector('button[type="submit"]');

const MESSAGES = {
  name: { valueMissing: "Enter your name." },
  email: {
    valueMissing: "Enter your email address.",
    typeMismatch: "Enter an email address like name@example.com.",
  },
  rating: { valueMissing: "Choose a rating." },
  message: { valueMissing: "Write a message.", tooShort: "Write at least 10 characters." },
};

/** The message for one field (for the rating, any of its radios), or "" when it is valid. */
export function errorFor(field) {
  const messages = MESSAGES[field.name] ?? {};
  const { validity } = field;
  if (validity.valueMissing || (field.required && field.value.trim() === "" && field.type !== "radio")) {
    return messages.valueMissing ?? "This field is required.";
  }
  if (validity.typeMismatch) {
    return messages.typeMismatch ?? "This value has the wrong format.";
  }
  // tooShort is only set after the user edits the field, so check minlength directly too.
  if (validity.tooShort || (field.minLength > 0 && field.value.trim().length < field.minLength)) {
    return messages.tooShort ?? `Write at least ${field.minLength} characters.`;
  }
  return "";
}

function controlsOf(form, name) {
  return [...form.elements].filter((element) => element.name === name);
}

/** Shows or clears the error of one field (by name) and sets aria-invalid; true when valid. */
export function checkField(form, name) {
  const controls = controlsOf(form, name);
  const message = errorFor(controls[0]);
  document.getElementById(`${name}-error`).textContent = message;
  const target = name === "rating" ? controls[0].closest("fieldset") : controls[0];
  if (message) {
    target.setAttribute("aria-invalid", "true");
  } else {
    target.removeAttribute("aria-invalid");
  }
  return message === "";
}

/** Checks every field; returns the first invalid control (to focus), or null. */
export function validate(form) {
  let first = null;
  for (const name of FIELDS) {
    if (!checkField(form, name) && first === null) {
      first = controlsOf(form, name)[0];
    }
  }
  return first;
}

/** The form's values as { name, email, rating, message } (strings; rating "" if none). */
export function formValues(form) {
  const data = new FormData(form);
  return Object.fromEntries(FIELDS.map((name) => [name, String(data.get(name) ?? "")]));
}

/** Saves formValues(form) as JSON in localStorage under DRAFT_KEY. */
export function saveDraft(form) {
  try {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(formValues(form)));
  } catch {
    // Storage full or disabled: the form still works.
  }
}

/** Fills the form from the saved draft, if there is one (and ignores a broken one). */
export function loadDraft(form) {
  let draft;
  try {
    draft = JSON.parse(localStorage.getItem(DRAFT_KEY) ?? "null");
  } catch {
    return;
  }
  if (draft === null || typeof draft !== "object") {
    return;
  }
  for (const name of ["name", "email", "message"]) {
    if (typeof draft[name] === "string") {
      form.elements[name].value = draft[name];
    }
  }
  for (const radio of controlsOf(form, "rating")) {
    radio.checked = radio.value === draft.rating;
  }
}

/** POSTs the values as JSON to /api/feedback; returns the response JSON, throws if not ok. */
export async function sendFeedback(values) {
  const response = await fetch("/api/feedback", {
    method: "POST",
    headers: { "content-type": "application/json", accept: "application/json" },
    body: JSON.stringify(values),
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}

function updateCounter() {
  counter.textContent = `${form.elements.message.value.length} / 500`;
}

loadDraft(form);
updateCounter();

form.addEventListener("input", (event) => {
  saveDraft(form);
  updateCounter();
  const name = event.target.name;
  const marked = name === "rating" ? event.target.closest("fieldset") : event.target;
  if (marked?.getAttribute("aria-invalid") === "true") {
    checkField(form, name);
  }
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const invalid = validate(form);
  if (invalid) {
    status.textContent = "Please fix the errors in the form.";
    invalid.focus();
    return;
  }
  const values = formValues(form);
  submit.disabled = true;
  status.textContent = "Sending…";
  try {
    await sendFeedback(values);
    status.textContent = `Thanks, ${values.name.trim()}! Your feedback was sent.`;
    form.reset();
    localStorage.removeItem(DRAFT_KEY);
    updateCounter();
  } catch {
    status.textContent = "Sending failed. Your message is saved; try again.";
  } finally {
    submit.disabled = false;
  }
});

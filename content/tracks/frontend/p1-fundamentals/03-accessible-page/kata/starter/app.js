const toggle = document.querySelector(".details-toggle");
const panel = document.querySelector("#talk-details");

toggle.addEventListener("click", () => {
  panel.hidden = !panel.hidden;
  toggle.textContent = panel.hidden ? "Show details" : "Hide details";
  // todo: tell assistive technology whether the panel is open (aria-expanded)
});

const form = document.querySelector(".newsletter");
const status = document.querySelector(".newsletter-status");

form.addEventListener("submit", (event) => {
  event.preventDefault();
  status.textContent = "Thanks! Check your inbox to confirm.";
});

const toggle = document.querySelector(".details-toggle");
const panel = document.querySelector("#talk-details");

toggle.addEventListener("click", () => {
  panel.hidden = !panel.hidden;
  toggle.textContent = panel.hidden ? "Show details" : "Hide details";
  toggle.setAttribute("aria-expanded", String(!panel.hidden));
});

const form = document.querySelector(".newsletter");
const status = document.querySelector(".newsletter-status");

form.addEventListener("submit", (event) => {
  event.preventDefault();
  status.textContent = "Thanks! Check your inbox to confirm.";
});

// <quantity-input>: a custom element that enhances the number input inside it with − and +
// buttons. Without JavaScript the plain input still works. See README.md.

export class QuantityInput extends HTMLElement {
  #input = null;
  #decrease = null;
  #increase = null;
  #last = 0;

  /** The quantity as a number. Setting it clamps it and updates the buttons, without an event. */
  get value() {
    return Number(this.#input?.value ?? 0);
  }

  set value(next) {
    this.#apply(next);
    this.#last = this.value;
  }

  connectedCallback() {
    // Runs again when the element is moved: enhance only once.
    if (this.#input) {
      return;
    }
    const input = this.querySelector('input[type="number"]');
    if (!input) {
      return;
    }
    this.#input = input;
    const label = input.labels?.[0]?.textContent.trim() || input.name;
    this.#decrease = this.#button("decrease", `Decrease ${label}`, "−");
    this.#increase = this.#button("increase", `Increase ${label}`, "+");
    input.before(this.#decrease);
    input.after(this.#increase);
    this.#decrease.addEventListener("click", () => this.#commit(this.value - 1));
    this.#increase.addEventListener("click", () => this.#commit(this.value + 1));
    input.addEventListener("change", () => this.#commit(input.value));
    this.value = input.value;
  }

  #button(className, name, text) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = className;
    button.setAttribute("aria-label", name);
    button.textContent = text;
    return button;
  }

  get #min() {
    const min = Number.parseInt(this.#input.min, 10);
    return Number.isNaN(min) ? 0 : min;
  }

  get #max() {
    const max = Number.parseInt(this.#input.max, 10);
    return Number.isNaN(max) ? Infinity : max;
  }

  #apply(raw) {
    const parsed = Number.parseInt(String(raw), 10);
    const value = Math.min(this.#max, Math.max(this.#min, Number.isNaN(parsed) ? this.#min : parsed));
    this.#input.value = String(value);
    this.#decrease.disabled = value <= this.#min;
    this.#increase.disabled = value >= this.#max;
  }

  #commit(raw) {
    this.#apply(raw);
    const value = this.value;
    if (value !== this.#last) {
      this.#last = value;
      this.dispatchEvent(
        new CustomEvent("quantity-change", { bubbles: true, detail: { name: this.#input.name, value } }),
      );
    }
  }
}

if (!customElements.get("quantity-input")) {
  customElements.define("quantity-input", QuantityInput);
}

// <quantity-input>: a custom element that enhances the number input inside it with − and +
// buttons. Without JavaScript the plain input still works. See README.md.

export class QuantityInput extends HTMLElement {
  /** The quantity as a number. Setting it clamps it and updates the buttons, without an event. */
  get value() {
    throw new Error("todo: write the value getter");
  }

  set value(next) {
    throw new Error("todo: write the value setter");
  }

  connectedCallback() {
    // todo: find the input, add the two buttons once (the element can be moved, and then this
    // runs again), clamp typed values on change and send "quantity-change" when the value changes
    throw new Error("todo: write connectedCallback()");
  }
}

// todo: register the element as "quantity-input", unless it is already registered

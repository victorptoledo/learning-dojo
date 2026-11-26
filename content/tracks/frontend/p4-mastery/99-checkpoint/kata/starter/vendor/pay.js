// Stands in for a payment provider's script, which in production comes from the provider's
// servers and can be slow, blocked or down. Done: do not change.
window.Pay = {
  mount(element) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "pay-button";
    button.textContent = "Pay with card";
    element.append(button);
  },
};

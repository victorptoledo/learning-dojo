// The size guide: in the real shop this module pulls in a measuring widget and weighs about
// 80 KB, so it should only be downloaded by the visitors who open the guide. Done: do not change.

const SIZES = [
  ["S", "96", "70"],
  ["M", "104", "72"],
  ["L", "112", "74"],
  ["XL", "120", "76"],
];

/** Fills `container` with the size table (built with the DOM API, no HTML strings). */
export function renderSizeGuide(container) {
  const table = document.createElement("table");
  const caption = table.createCaption();
  caption.textContent = "Size guide (cm)";
  const head = table.createTHead().insertRow();
  for (const title of ["Size", "Chest", "Length"]) {
    const th = document.createElement("th");
    th.scope = "col";
    th.textContent = title;
    head.append(th);
  }
  const body = table.createTBody();
  for (const [size, chest, length] of SIZES) {
    const row = body.insertRow();
    const th = document.createElement("th");
    th.scope = "row";
    th.textContent = size;
    row.append(th);
    row.insertCell().textContent = chest;
    row.insertCell().textContent = length;
  }
  container.replaceChildren(table);
}

Start with the table and the order. Export `BillSplitter` (no props) from `BillSplitter.tsx`.

**People.** A form named "Add a person" (`<form aria-label="Add a person">`) with a text
field labelled `Name` and a submit button `Add person`.

- The name is trimmed. An empty name gives the error `Enter a name`; a name already on the
  list, compared without case, gives `<name as typed, trimmed> is already on the list`.
- A valid name is added at the end of a list named `People` (`<ul aria-label="People">`),
  where each person has a button `Remove <name>`. The field is emptied.
- With nobody at the table the list is not shown and the text `No people yet` is.

**Items.** A form named "Add an item" with two text fields, `Item` and `Price`, and a submit
button `Add item`.

- The description is trimmed. Empty: `Enter an item`; already on the bill, without case:
  `<description> is already on the bill`.
- The price is trimmed. Empty: `Enter a price`. It must be digits with an optional `.` or
  `,` and one or two more digits (`12`, `12.5`, `12,50`); anything else (`abc`, `12.345`,
  `1e3`, `-4`): `Enter a price like 12.50`. Zero: `The price must be more than 0`.
- Both fields are checked on every submit. A valid item goes at the end of a list named
  `Items`; each row shows the description and the price with two decimals and a `.`
  (`12.50`), each in its own element, and a button `Remove <description>`. Both fields are
  emptied. With no items the list is not shown and the text `No items yet` is.
- Below, the text `Subtotal: <sum>` (`Subtotal: 0.00` at first).

**Errors are for everyone.** A field with an error has `aria-invalid="true"` and points at
the element with the message through `aria-describedby`, so a screen reader reads it with
the field. After a submit, the focus goes to the first field with an error, or, when the
form was valid, back to its first field (`Name` or `Item`), ready for the next one. The
errors of a form stay until its next submit, which shows only the errors it finds.

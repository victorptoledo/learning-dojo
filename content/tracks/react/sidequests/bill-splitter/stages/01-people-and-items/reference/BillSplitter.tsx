import { useId, useRef, useState, type FormEvent, type ReactNode, type RefObject } from "react";

type Person = { id: number; name: string };
type Item = { id: number; description: string; cents: number };

const PRICE = /^\d+([.,]\d{1,2})?$/;

function parseCents(text: string): number {
  const [whole = "0", fraction = ""] = text.split(/[.,]/);
  return Number(whole) * 100 + Number(fraction.padEnd(2, "0"));
}

function money(cents: number): string {
  return `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, "0")}`;
}

type FieldProps = {
  label: string;
  value: string;
  error: string | undefined;
  inputRef: RefObject<HTMLInputElement | null>;
  onChange: (value: string) => void;
  inputMode?: "decimal";
};

function Field({ label, value, error, inputRef, onChange, inputMode }: FieldProps): ReactNode {
  const id = useId();
  return (
    <p>
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        ref={inputRef}
        value={value}
        inputMode={inputMode}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {error ? <span id={`${id}-error`}>{error}</span> : null}
    </p>
  );
}

/** The bill: who is at the table and what was ordered. */
export function BillSplitter(): ReactNode {
  const [people, setPeople] = useState<readonly Person[]>([]);
  const [items, setItems] = useState<readonly Item[]>([]);
  const [name, setName] = useState("");
  const [nameError, setNameError] = useState<string>();
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [itemErrors, setItemErrors] = useState<{ description?: string; price?: string }>({});
  const nextId = useRef(1);
  const nameRef = useRef<HTMLInputElement>(null);
  const descriptionRef = useRef<HTMLInputElement>(null);
  const priceRef = useRef<HTMLInputElement>(null);

  function addPerson(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = name.trim();
    let error: string | undefined;
    if (trimmed === "") {
      error = "Enter a name";
    } else if (people.some((person) => person.name.toLowerCase() === trimmed.toLowerCase())) {
      error = `${trimmed} is already on the list`;
    }
    setNameError(error);
    if (!error) {
      setPeople([...people, { id: nextId.current++, name: trimmed }]);
      setName("");
    }
    nameRef.current?.focus();
  }

  function addItem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const text = description.trim();
    const amount = price.trim();
    const errors: { description?: string; price?: string } = {};
    if (text === "") {
      errors.description = "Enter an item";
    } else if (items.some((item) => item.description.toLowerCase() === text.toLowerCase())) {
      errors.description = `${text} is already on the bill`;
    }
    if (amount === "") {
      errors.price = "Enter a price";
    } else if (!PRICE.test(amount)) {
      errors.price = "Enter a price like 12.50";
    } else if (parseCents(amount) === 0) {
      errors.price = "The price must be more than 0";
    }
    setItemErrors(errors);
    if (errors.description) {
      descriptionRef.current?.focus();
      return;
    }
    if (errors.price) {
      priceRef.current?.focus();
      return;
    }
    setItems([...items, { id: nextId.current++, description: text, cents: parseCents(amount) }]);
    setDescription("");
    setPrice("");
    descriptionRef.current?.focus();
  }

  const subtotal = items.reduce((sum, item) => sum + item.cents, 0);

  return (
    <main>
      <h1>Split the bill</h1>

      <section>
        <h2>People</h2>
        <form aria-label="Add a person" onSubmit={addPerson} noValidate>
          <Field label="Name" value={name} error={nameError} inputRef={nameRef} onChange={setName} />
          <button type="submit">Add person</button>
        </form>
        {people.length === 0 ? (
          <p>No people yet</p>
        ) : (
          <ul aria-label="People">
            {people.map((person) => (
              <li key={person.id}>
                <span>{person.name}</span>{" "}
                <button type="button" onClick={() => setPeople(people.filter((other) => other.id !== person.id))}>
                  Remove {person.name}
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2>Items</h2>
        <form aria-label="Add an item" onSubmit={addItem} noValidate>
          <Field
            label="Item"
            value={description}
            error={itemErrors.description}
            inputRef={descriptionRef}
            onChange={setDescription}
          />
          <Field
            label="Price"
            value={price}
            error={itemErrors.price}
            inputRef={priceRef}
            onChange={setPrice}
            inputMode="decimal"
          />
          <button type="submit">Add item</button>
        </form>
        {items.length === 0 ? (
          <p>No items yet</p>
        ) : (
          <ul aria-label="Items">
            {items.map((item) => (
              <li key={item.id}>
                <span>{item.description}</span> <span>{money(item.cents)}</span>{" "}
                <button type="button" onClick={() => setItems(items.filter((other) => other.id !== item.id))}>
                  Remove {item.description}
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2>Totals</h2>
        <p>{`Subtotal: ${money(subtotal)}`}</p>
      </section>
    </main>
  );
}

import { useId, useRef, useState, type FormEvent, type ReactNode, type RefObject } from "react";

type Person = { id: number; name: string };
type Item = { id: number; description: string; cents: number; people: readonly number[] };
type Tip = "none" | "10" | "15" | "custom";

const PRICE = /^\d+([.,]\d{1,2})?$/;
const WHOLE = /^\d+$/;

function parseCents(text: string): number {
  const [whole = "0", fraction = ""] = text.split(/[.,]/);
  return Number(whole) * 100 + Number(fraction.padEnd(2, "0"));
}

function money(cents: number): string {
  return `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, "0")}`;
}

/** Splits `cents` in proportion to `weights`, largest remainder first, ties in order. */
function splitByWeight(cents: number, weights: readonly number[]): number[] {
  const total = weights.reduce((sum, weight) => sum + weight, 0);
  if (total === 0) {
    return weights.map(() => 0);
  }
  const parts = weights.map((weight) => Math.floor((cents * weight) / total));
  const remainders = weights.map((weight, index) => ({ index, rest: (cents * weight) % total }));
  remainders.sort((a, b) => b.rest - a.rest || a.index - b.index);
  let left = cents - parts.reduce((sum, part) => sum + part, 0);
  for (const { index } of remainders) {
    if (left === 0) {
      break;
    }
    parts[index] = (parts[index] ?? 0) + 1;
    left -= 1;
  }
  return parts;
}

function tipPercent(tip: Tip, custom: string): number | null {
  if (tip === "none") {
    return 0;
  }
  if (tip !== "custom") {
    return Number(tip);
  }
  const text = custom.trim();
  if (text === "") {
    return 0;
  }
  return WHOLE.test(text) && Number(text) <= 100 ? Number(text) : null;
}

type FieldProps = {
  label: string;
  value: string;
  error: string | undefined;
  inputRef: RefObject<HTMLInputElement | null>;
  onChange: (value: string) => void;
  inputMode?: "decimal" | "numeric";
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

/** The whole bill: people, items, who had what, the tip and what each person owes. */
export function BillSplitter(): ReactNode {
  const [people, setPeople] = useState<readonly Person[]>([]);
  const [items, setItems] = useState<readonly Item[]>([]);
  const [name, setName] = useState("");
  const [nameError, setNameError] = useState<string>();
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [itemErrors, setItemErrors] = useState<{ description?: string; price?: string }>({});
  const [tip, setTip] = useState<Tip>("none");
  const [customTip, setCustomTip] = useState("");
  const nextId = useRef(1);
  const nameRef = useRef<HTMLInputElement>(null);
  const descriptionRef = useRef<HTMLInputElement>(null);
  const priceRef = useRef<HTMLInputElement>(null);
  const tipName = useId();

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

  function removePerson(id: number) {
    setPeople(people.filter((person) => person.id !== id));
    setItems(items.map((item) => ({ ...item, people: item.people.filter((who) => who !== id) })));
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
    const item = {
      id: nextId.current++,
      description: text,
      cents: parseCents(amount),
      people: people.map((person) => person.id),
    };
    setItems([...items, item]);
    setDescription("");
    setPrice("");
    descriptionRef.current?.focus();
  }

  function toggle(itemId: number, personId: number) {
    setItems(
      items.map((item) => {
        if (item.id !== itemId) {
          return item;
        }
        const has = item.people.includes(personId);
        return {
          ...item,
          people: has ? item.people.filter((who) => who !== personId) : [...item.people, personId],
        };
      }),
    );
  }

  const subtotal = items.reduce((sum, item) => sum + item.cents, 0);
  const shares = new Map(people.map((person) => [person.id, 0]));
  let unassigned = 0;
  for (const item of items) {
    const eaters = people.filter((person) => item.people.includes(person.id));
    if (eaters.length === 0) {
      unassigned += item.cents;
      continue;
    }
    const parts = splitByWeight(
      item.cents,
      eaters.map(() => 1),
    );
    eaters.forEach((person, index) => {
      shares.set(person.id, (shares.get(person.id) ?? 0) + (parts[index] ?? 0));
    });
  }
  const percent = tipPercent(tip, customTip);
  const tipCents = Math.round((subtotal * (percent ?? 0)) / 100);
  const weights = people.map((person) => shares.get(person.id) ?? 0);
  const tips = splitByWeight(tipCents, weights);
  if (weights.every((weight) => weight === 0)) {
    unassigned += tipCents;
  }

  const tipOptions: { value: Tip; label: string }[] = [
    { value: "none", label: "No tip" },
    { value: "10", label: "10%" },
    { value: "15", label: "15%" },
    { value: "custom", label: "Custom" },
  ];

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
                <button type="button" onClick={() => removePerson(person.id)}>
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
                {people.length > 0 ? (
                  <fieldset>
                    <legend>{`Who had ${item.description}?`}</legend>
                    {people.map((person) => (
                      <label key={person.id}>
                        <input
                          type="checkbox"
                          checked={item.people.includes(person.id)}
                          onChange={() => toggle(item.id, person.id)}
                        />
                        {person.name}
                      </label>
                    ))}
                  </fieldset>
                ) : null}
                {item.people.length === 0 ? <p>{`Nobody had ${item.description}`}</p> : null}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section>
        <h2>Tip</h2>
        <fieldset>
          <legend>Tip</legend>
          {tipOptions.map((option) => (
            <label key={option.value}>
              <input
                type="radio"
                name={tipName}
                value={option.value}
                checked={tip === option.value}
                onChange={() => setTip(option.value)}
              />
              {option.label}
            </label>
          ))}
        </fieldset>
        {tip === "custom" ? (
          <CustomTip value={customTip} onChange={setCustomTip} invalid={percent === null} />
        ) : null}
      </section>

      <section>
        <h2>Totals</h2>
        <p>{`Subtotal: ${money(subtotal)}`}</p>
        <p>{`Tip: ${money(tipCents)}`}</p>
        <p>{`Total: ${money(subtotal + tipCents)}`}</p>
        {unassigned > 0 ? <p>{`Unassigned: ${money(unassigned)}`}</p> : null}
        {people.length > 0 ? (
          <ul aria-label="Shares">
            {people.map((person, index) => (
              <li key={person.id}>
                {`${person.name} owes ${money((shares.get(person.id) ?? 0) + (tips[index] ?? 0))}`}
              </li>
            ))}
          </ul>
        ) : null}
      </section>
    </main>
  );
}

function CustomTip(props: { value: string; invalid: boolean; onChange: (value: string) => void }): ReactNode {
  const ref = useRef<HTMLInputElement>(null);
  return (
    <Field
      label="Custom tip (%)"
      value={props.value}
      error={props.invalid ? "Enter a tip from 0 to 100" : undefined}
      inputRef={ref}
      onChange={props.onChange}
      inputMode="numeric"
    />
  );
}

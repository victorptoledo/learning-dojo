import { useState } from "react";
import { sortContacts, type Contact } from "./contacts.ts";
import { trackRender } from "./renderLog.ts";

type ContactRowProps = {
  contact: Contact;
  selected: boolean;
  onSelect: (id: string) => void;
};

function ContactRow({ contact, selected, onSelect }: ContactRowProps) {
  trackRender(`row:${contact.id}`);
  return (
    <li>
      <button type="button" aria-pressed={selected} onClick={() => onSelect(contact.id)}>
        {contact.name}
      </button>
    </li>
  );
}

export type ContactsPageProps = {
  contacts: readonly Contact[];
};

/** A note, a filter, the sorted contacts and the selected one. Works, but wastes renders. */
export function ContactsPage({ contacts }: ContactsPageProps) {
  const [note, setNote] = useState("");
  const [filter, setFilter] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const sorted = sortContacts(contacts);
  const needle = filter.trim().toLowerCase();
  const visible = sorted.filter((contact) => contact.name.toLowerCase().includes(needle));
  const selected = contacts.find((contact) => contact.id === selectedId);

  return (
    <div>
      <label>
        Note <input value={note} onChange={(event) => setNote(event.target.value)} />
      </label>
      <label>
        Filter <input value={filter} onChange={(event) => setFilter(event.target.value)} />
      </label>
      <ul aria-label="Contacts">
        {visible.map((contact) => (
          <ContactRow
            key={contact.id}
            contact={contact}
            selected={contact.id === selectedId}
            onSelect={(id) => setSelectedId(id)}
          />
        ))}
      </ul>
      <p>Selected: {selected ? selected.name : "nobody"}</p>
    </div>
  );
}

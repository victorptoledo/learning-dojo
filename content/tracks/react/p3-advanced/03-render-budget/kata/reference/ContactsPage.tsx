import { memo, useCallback, useMemo, useState } from "react";
import { sortContacts, type Contact } from "./contacts.ts";
import { trackRender } from "./renderLog.ts";

type ContactRowProps = {
  contact: Contact;
  selected: boolean;
  onSelect: (id: string) => void;
};

// memo: a row renders again only when its contact, selected flag or handler changes.
const ContactRow = memo(function ContactRow({ contact, selected, onSelect }: ContactRowProps) {
  trackRender(`row:${contact.id}`);
  return (
    <li>
      <button type="button" aria-pressed={selected} onClick={() => onSelect(contact.id)}>
        {contact.name}
      </button>
    </li>
  );
});

// The note is colocated: typing in it no longer renders the page.
function NoteField() {
  const [note, setNote] = useState("");
  return (
    <label>
      Note <input value={note} onChange={(event) => setNote(event.target.value)} />
    </label>
  );
}

export type ContactsPageProps = {
  contacts: readonly Contact[];
};

/** A note, a filter, the sorted contacts and the selected one. */
export function ContactsPage({ contacts }: ContactsPageProps) {
  const [filter, setFilter] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const sorted = useMemo(() => sortContacts(contacts), [contacts]);
  const needle = filter.trim().toLowerCase();
  const visible = useMemo(
    () => sorted.filter((contact) => contact.name.toLowerCase().includes(needle)),
    [sorted, needle],
  );
  const handleSelect = useCallback((id: string) => setSelectedId(id), []);
  const selected = contacts.find((contact) => contact.id === selectedId);

  return (
    <div>
      <NoteField />
      <label>
        Filter <input value={filter} onChange={(event) => setFilter(event.target.value)} />
      </label>
      <ul aria-label="Contacts">
        {visible.map((contact) => (
          <ContactRow
            key={contact.id}
            contact={contact}
            selected={contact.id === selectedId}
            onSelect={handleSelect}
          />
        ))}
      </ul>
      <p>Selected: {selected ? selected.name : "nobody"}</p>
    </div>
  );
}

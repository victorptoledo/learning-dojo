// The data and the "expensive" sort. Do not change this file.
import { trackRender } from "./renderLog.ts";

export type Contact = {
  readonly id: string;
  readonly name: string;
  readonly email: string;
};

/** A sorted copy (by name); counted as "sortContacts" each time it runs. */
export function sortContacts(contacts: readonly Contact[]): readonly Contact[] {
  trackRender("sortContacts");
  return contacts.toSorted((a, b) => a.name.localeCompare(b.name));
}

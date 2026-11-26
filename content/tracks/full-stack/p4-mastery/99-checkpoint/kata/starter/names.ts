// How one full name maps to a first and a last name, and back.

export interface SplitName {
  firstName: string;
  lastName: string;
}

/** The first word is the first name; every other word, joined by one space, is the last name. */
export function splitFullName(fullName: string): SplitName {
  throw new Error(`todo: split ${JSON.stringify(fullName)}`);
}

/** The full name for old clients: both parts trimmed, joined by one space, no space when one is empty. */
export function joinName(firstName: string, lastName: string): string {
  throw new Error(`todo: join ${JSON.stringify([firstName, lastName])}`);
}

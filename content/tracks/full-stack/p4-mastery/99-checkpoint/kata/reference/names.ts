// How one full name maps to a first and a last name, and back.

export interface SplitName {
  firstName: string;
  lastName: string;
}

/** The first word is the first name; every other word, joined by one space, is the last name. */
export function splitFullName(fullName: string): SplitName {
  const words = fullName.trim().split(/\s+/).filter((word) => word !== "");
  return { firstName: words[0] ?? "", lastName: words.slice(1).join(" ") };
}

/** The full name for old clients: both parts trimmed, joined by one space, no space when one is empty. */
export function joinName(firstName: string, lastName: string): string {
  return [firstName.trim(), lastName.trim()].filter((part) => part !== "").join(" ");
}

// Feature flags with a kill switch, an allow list and a stable percentage rollout.

export interface Flag {
  name: string;
  /** The kill switch: false turns the flag off for everyone, the allow list included. */
  enabled: boolean;
  /** 0 to 100: the share of users that get the flag. */
  percent: number;
  /** Users that always get the flag while it is enabled (the team, beta testers). */
  allow?: number[];
}

/** 32-bit FNV-1a over the UTF-8 bytes of `text`, as an unsigned number. */
export function fnv1a(text: string): number {
  let hash = 0x811c9dc5;
  for (const byte of new TextEncoder().encode(text)) {
    hash ^= byte;
    hash = Math.imul(hash, 0x01000193) >>> 0;
  }
  return hash >>> 0;
}

/** The user's bucket for this flag, 0 to 99: the same every time, and different from flag to flag. */
export function bucket(flagName: string, userId: number): number {
  return fnv1a(`${flagName}:${userId}`) % 100;
}

export function isEnabled(flag: Flag | undefined, userId: number): boolean {
  if (!flag || !flag.enabled) return false;
  if (flag.allow?.includes(userId)) return true;
  return bucket(flag.name, userId) < flag.percent;
}

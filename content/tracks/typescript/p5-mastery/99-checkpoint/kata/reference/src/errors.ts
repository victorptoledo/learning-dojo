/** Every failure of the SDK: invalid definitions, invalid overrides, a failed refresh. */
export class FlagsError extends Error {
  override readonly name = "FlagsError";
  /** "path: message" lines when the failure comes from validation; empty otherwise. */
  readonly issues: readonly string[];

  constructor(message: string, options: { cause?: unknown; issues?: readonly string[] } = {}) {
    super(message, { cause: options.cause });
    this.issues = options.issues ?? [];
  }
}

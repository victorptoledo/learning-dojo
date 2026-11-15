// Your first TypeScript kata: a greeting and a tiny command line around it.

export interface GreetOptions {
  /** Upper-case the whole message. */
  shout?: boolean;
}

/** "Welcome to the dojo, <name>!", with "stranger" when the name is missing or blank. */
export function greet(name?: string, options: GreetOptions = {}): string {
  throw new Error("todo: write greet()");
}

export interface Command {
  name: string;
  shout: boolean;
}

/** Reads the arguments: "--shout" anywhere turns shouting on, the other words form the name. */
export function parseArgs(argv: readonly string[]): Command {
  throw new Error("todo: write parseArgs()");
}

/** Prints the greeting for argv with log, then returns the exit code 0. */
export function main(argv: readonly string[], log: (line: string) => void = console.log): number {
  throw new Error("todo: write main()");
}

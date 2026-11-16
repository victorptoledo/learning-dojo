// A node:test reporter that writes one JSON line per finished test: { name, passed, message }.
// report.mjs reads the lines and prints them in `cargo test` format.
import path from "node:path";

const MAX_MESSAGE_LINES = 8;

/** The short reason a test failed: the original error, not the runner's wrapper. */
function messageOf(error) {
  let cause = error;
  while (cause && cause.code === "ERR_TEST_FAILURE" && cause.cause) {
    cause = cause.cause;
  }
  let text;
  if (cause instanceof Error) {
    text = cause.message || cause.name;
  } else {
    text = String(cause ?? "the test failed");
  }
  const lines = text
    .split("\n")
    .map((line) => line.trimEnd())
    .filter((line) => line.trim() !== "" && !line.startsWith("+ actual - expected"));
  return lines.slice(0, MAX_MESSAGE_LINES).join("\n") || "the test failed";
}

function stemOf(file) {
  if (!file) {
    return "tests";
  }
  return path
    .basename(file)
    .replace(/\.test\.(c|m)?[jt]sx?$/, "")
    .replace(/\.(c|m)?[jt]sx?$/, "");
}

const clean = (text) => text.trim().replace(/\s+/g, "_");

export default async function* cargoReporter(source) {
  // Files run in parallel, so their events interleave: one stack of suite names per file.
  const stacks = new Map();
  for await (const event of source) {
    const data = event.data ?? {};
    const file = data.file ?? "";
    if (event.type === "test:start") {
      const stack = stacks.get(file) ?? [];
      stack.length = data.nesting;
      stack.push(data.name);
      stacks.set(file, stack);
      continue;
    }
    if (event.type !== "test:pass" && event.type !== "test:fail") {
      continue;
    }
    const details = data.details ?? {};
    // A suite fails when one of its tests fails: that test is already reported.
    if (
      details.type === "suite" &&
      (event.type === "test:pass" || details.error?.failureType === "subtestsFailed")
    ) {
      continue;
    }
    if (data.skip || data.todo) {
      continue;
    }
    const suites = (stacks.get(file) ?? []).slice(0, data.nesting);
    const name = [stemOf(file), ...suites, data.name].map(clean).join("::");
    const passed = event.type === "test:pass";
    const message = passed ? "" : messageOf(details.error);
    yield JSON.stringify({ name, passed, message }) + "\n";
  }
}

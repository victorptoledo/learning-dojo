// Checks the Frontend kata in the current folder and prints the result in `cargo test` format.
//
// tests/**/*.test.js run with node:test and cargo-reporter.mjs, one file at a time (each file
// drives one headless Chromium through the dojo-web helper, and the container has few pids).
//
// Exits 0 when everything passed, 1 when something failed, 2 when no test ran.
import { spawnSync } from "node:child_process";
import { existsSync, readdirSync } from "node:fs";
import path from "node:path";

const MAX_LOG_LINES = 40;

function filesUnder(dir, suffix) {
  if (!existsSync(dir)) {
    return [];
  }
  const found = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      found.push(...filesUnder(full, suffix));
    } else if (entry.name.endsWith(suffix)) {
      found.push(full.split(path.sep).join("/"));
    }
  }
  return found.sort();
}

function runTests(files) {
  const run = spawnSync(
    process.execPath,
    [
      "--test",
      "--test-concurrency=1",
      "--test-reporter=/opt/dojo/cargo-reporter.mjs",
      "--test-reporter-destination=stdout",
      "--test-timeout=20000",
      "--disable-warning=ExperimentalWarning",
      ...files,
    ],
    { encoding: "utf8", maxBuffer: 16 * 1024 * 1024 },
  );
  const results = [];
  for (const line of (run.stdout ?? "").split("\n")) {
    if (line.startsWith("{")) {
      try {
        results.push(JSON.parse(line));
      } catch {
        // Not one of the reporter's lines.
      }
    }
  }
  return { results, log: `${run.stdout ?? ""}${run.stderr ?? ""}` };
}

function print(results) {
  console.log(`\nrunning ${results.length} tests`);
  for (const result of results) {
    console.log(`test ${result.name} ... ${result.passed ? "ok" : "FAILED"}`);
  }
  const failures = results.filter((result) => !result.passed);
  const passed = results.length - failures.length;
  if (failures.length === 0) {
    console.log(`\ntest result: ok. ${passed} passed; 0 failed`);
    return 0;
  }
  console.log("\nfailures:\n");
  for (const { name, message } of failures) {
    console.log(`---- ${name} stdout ----`);
    console.log(`thread '${name}' panicked at ${name}:`);
    console.log(`${message || "the test failed"}\n`);
  }
  console.log(`failures:\n\ntest result: FAILED. ${passed} passed; ${failures.length} failed`);
  return 1;
}

function main() {
  // The visible tests first, so their results lead the report.
  const files = filesUnder("tests", ".test.js").sort(
    (a, b) => Number(!a.endsWith("/visible.test.js")) - Number(!b.endsWith("/visible.test.js")),
  );
  if (files.length === 0) {
    console.log("error: no tests found (tests/*.test.js)");
    return 2;
  }
  const { results, log } = runTests(files);
  if (results.length === 0) {
    console.log("error: no tests ran");
    console.log(log.split("\n").slice(0, MAX_LOG_LINES).join("\n"));
    return 2;
  }
  return print(results);
}

process.exitCode = main();

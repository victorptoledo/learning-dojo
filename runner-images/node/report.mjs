// Checks the kata in the current folder and prints the result in `cargo test` format:
//
// 1. `tsc` type-checks everything with tsconfig.dojo.json. An error in the learner's code or in
//    a runtime test file means the code "does not compile".
// 2. Each `// test: name` section of tests/*.types.ts is a type-level test: it passes when tsc
//    reports no error inside it (`@ts-expect-error` lines must really be errors).
// 3. tests/**/*.test.ts run with node:test (Node.js strips the types) and cargo-reporter.mjs.
//
// Exits 0 when everything passed, 1 when something failed or did not compile, 2 when no test ran.
import { spawnSync } from "node:child_process";
import { readFileSync, readdirSync, existsSync } from "node:fs";
import path from "node:path";

const MAX_MESSAGE_LINES = 8;
const MAX_COMPILE_LINES = 40;
const DIAGNOSTIC = /^(.+?)\((\d+),(\d+)\): error (TS\d+): (.*)$/;
const SECTION = /^\s*\/\/\s*test:\s*(.+?)\s*$/;

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

/** tsc diagnostics as { file, line, text }, continuation lines included in text. */
function typeCheck() {
  const run = spawnSync(
    "node_modules/.bin/tsc",
    ["-p", "tsconfig.dojo.json", "--pretty", "false"],
    { encoding: "utf8", maxBuffer: 16 * 1024 * 1024 },
  );
  const output = `${run.stdout ?? ""}${run.stderr ?? ""}`;
  if (run.error) {
    return { diagnostics: [], fatal: `error: cannot run tsc: ${run.error.message}` };
  }
  const diagnostics = [];
  for (const line of output.split("\n")) {
    const match = DIAGNOSTIC.exec(line);
    if (match) {
      const [, file, row, column, code, message] = match;
      diagnostics.push({
        file: path.normalize(file).split(path.sep).join("/"),
        line: Number(row),
        text: `${file}:${row}:${column} ${code}: ${message}`,
      });
    } else if (line.startsWith("error TS")) {
      diagnostics.push({ file: "", line: 0, text: line });
    } else if (line.startsWith(" ") && diagnostics.length > 0 && line.trim() !== "") {
      diagnostics.at(-1).text += `\n${line.trimEnd()}`;
    }
  }
  if (run.status !== 0 && diagnostics.length === 0) {
    return { diagnostics, fatal: `error: tsc failed\n${output.trim()}` };
  }
  return { diagnostics, fatal: null };
}

const clean = (text) => text.trim().replace(/\s+/g, "_");

/** One result per `// test:` section of each type test file. */
function typeTests(files, diagnostics) {
  const results = [];
  for (const file of files) {
    const stem = path.basename(file, ".types.ts");
    const lines = readFileSync(file, "utf8").split("\n");
    const sections = [{ name: "setup", start: 1 }];
    lines.forEach((line, index) => {
      const match = SECTION.exec(line);
      if (match) {
        sections.push({ name: match[1], start: index + 1 });
      }
    });
    sections.forEach((section, index) => {
      const end = sections[index + 1]?.start ?? Number.POSITIVE_INFINITY;
      const errors = diagnostics.filter(
        (found) => found.file === file && found.line >= section.start && found.line < end,
      );
      if (section.name === "setup" && errors.length === 0) {
        return;
      }
      const message = errors
        .flatMap((found) => found.text.split("\n"))
        .map((line) => line.replace(/^tests\//, ""))
        .slice(0, MAX_MESSAGE_LINES)
        .join("\n");
      results.push({
        name: ["types", stem, section.name].map(clean).join("::"),
        passed: errors.length === 0,
        message,
      });
    });
  }
  return results;
}

function runtimeTests(files) {
  if (files.length === 0) {
    return { results: [], log: "" };
  }
  const run = spawnSync(
    process.execPath,
    [
      "--test",
      "--test-reporter=/opt/dojo/cargo-reporter.mjs",
      "--test-reporter-destination=stdout",
      "--test-timeout=10000",
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
        // Not one of the reporter's lines: output printed by the code under test.
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
  const typeFiles = filesUnder("tests", ".types.ts");
  const testFiles = filesUnder("tests", ".test.ts");
  if (typeFiles.length === 0 && testFiles.length === 0) {
    console.log("error: no tests found (tests/*.test.ts or tests/*.types.ts)");
    return 2;
  }

  const { diagnostics, fatal } = typeCheck();
  if (fatal) {
    console.log(fatal);
    return 1;
  }
  const inTypeTests = new Set(typeFiles);
  const compileErrors = diagnostics.filter((found) => !inTypeTests.has(found.file));
  if (compileErrors.length > 0) {
    console.log("error: the code does not compile (tsc found type errors)");
    const lines = compileErrors.flatMap((found) => found.text.split("\n"));
    console.log(lines.slice(0, MAX_COMPILE_LINES).join("\n"));
    if (lines.length > MAX_COMPILE_LINES) {
      console.log(`... ${lines.length - MAX_COMPILE_LINES} more lines`);
    }
    return 1;
  }

  const runtime = runtimeTests(testFiles);
  if (testFiles.length > 0 && runtime.results.length === 0) {
    console.log("error: no tests ran");
    console.log(runtime.log.split("\n").slice(0, MAX_COMPILE_LINES).join("\n"));
    return 2;
  }
  return print([...typeTests(typeFiles, diagnostics), ...runtime.results]);
}

process.exitCode = main();

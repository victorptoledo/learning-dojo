import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { render, screen } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { Tabs, type Tab } from "../Tabs.tsx";

const TABS: readonly Tab[] = [
  { id: "profile", title: "Profile", content: <p>Profile form</p> },
  { id: "billing", title: "Billing", content: <p>Billing details</p>, disabled: true },
  { id: "security", title: "Security", content: <p>Security keys</p> },
  { id: "alerts", title: "Alerts", content: <p>Alert rules</p> },
];

function tab(name: string) {
  return screen.getByRole("tab", { name });
}

function selected() {
  return screen
    .getAllByRole("tab")
    .filter((element) => element.getAttribute("aria-selected") === "true")
    .map((element) => element.textContent);
}

describe("structure", () => {
  test("only the selected tab is aria-selected and its panel is shown", () => {
    render(<Tabs label="Settings" tabs={TABS} />);
    assert.deepEqual(selected(), ["Profile"]);
    assert.equal(tab("Security").getAttribute("aria-selected"), "false");
    const panels = screen.getAllByRole("tabpanel");
    assert.equal(panels.length, 1);
    assert.equal(panels[0]?.textContent, "Profile form");
  });

  test("tabs and panel point to each other by id", () => {
    render(<Tabs label="Settings" tabs={TABS} />);
    const panel = screen.getByRole("tabpanel");
    assert.ok(panel.id, "the panel has an id");
    assert.equal(tab("Profile").getAttribute("aria-controls"), panel.id);
    assert.equal(panel.getAttribute("aria-labelledby"), tab("Profile").id);
    assert.equal(screen.getByRole("tabpanel", { name: "Profile" }), panel);
    assert.equal(panel.getAttribute("tabindex"), "0");
  });

  test("ids are unique when two tab lists are on the page", () => {
    render(
      <>
        <Tabs label="First" tabs={TABS} />
        <Tabs label="Second" tabs={TABS} />
      </>,
    );
    const ids = screen.getAllByRole("tab").map((element) => element.id);
    assert.equal(new Set(ids).size, ids.length);
    assert.ok(ids.every((id) => id !== ""));
  });

  test("tabs are buttons that do not submit forms", () => {
    render(<Tabs label="Settings" tabs={TABS} />);
    for (const element of screen.getAllByRole("tab")) {
      assert.equal(element.tagName, "BUTTON");
      assert.equal(element.getAttribute("type"), "button");
    }
  });
});

describe("selection", () => {
  test("starts on defaultTabId, or on the first enabled tab", () => {
    const { unmount } = render(<Tabs label="Settings" tabs={TABS} defaultTabId="alerts" />);
    assert.deepEqual(selected(), ["Alerts"]);
    unmount();
    render(<Tabs label="Settings" tabs={TABS.toSpliced(0, 1)} />);
    assert.deepEqual(selected(), ["Security"]);
  });

  test("clicking selects a tab and calls onChange once per change", async () => {
    const user = userEvent.setup();
    const changes: string[] = [];
    render(<Tabs label="Settings" tabs={TABS} onChange={(id) => changes.push(id)} />);
    await user.click(tab("Security"));
    assert.deepEqual(selected(), ["Security"]);
    assert.equal(screen.getByRole("tabpanel").textContent, "Security keys");
    await user.click(tab("Security"));
    await user.click(tab("Profile"));
    assert.deepEqual(changes, ["security", "profile"]);
  });

  test("a disabled tab is marked and cannot be selected", async () => {
    const user = userEvent.setup();
    render(<Tabs label="Settings" tabs={TABS} />);
    assert.equal(tab("Billing").getAttribute("aria-disabled"), "true");
    await user.click(tab("Billing"));
    assert.deepEqual(selected(), ["Profile"]);
  });
});

describe("keyboard", () => {
  test("roving tabindex: Tab enters at the selected tab, then goes to the panel", async () => {
    const user = userEvent.setup();
    render(
      <>
        <button type="button">Before</button>
        <Tabs label="Settings" tabs={TABS} defaultTabId="security" />
      </>,
    );
    assert.deepEqual(
      screen.getAllByRole("tab").map((element) => element.getAttribute("tabindex")),
      ["-1", "-1", "0", "-1"],
    );
    await user.click(screen.getByRole("button", { name: "Before" }));
    await user.tab();
    assert.equal(document.activeElement, tab("Security"));
    await user.tab();
    assert.equal(document.activeElement, screen.getByRole("tabpanel"));
  });

  test("ArrowRight and ArrowLeft move focus and selection, skipping disabled tabs", async () => {
    const user = userEvent.setup();
    render(<Tabs label="Settings" tabs={TABS} />);
    await user.click(tab("Profile"));
    await user.keyboard("{ArrowRight}");
    assert.equal(document.activeElement, tab("Security"));
    assert.deepEqual(selected(), ["Security"]);
    await user.keyboard("{ArrowLeft}");
    assert.equal(document.activeElement, tab("Profile"));
    assert.deepEqual(selected(), ["Profile"]);
    assert.equal(tab("Profile").getAttribute("tabindex"), "0");
  });

  test("arrows wrap around at both ends", async () => {
    const user = userEvent.setup();
    render(<Tabs label="Settings" tabs={TABS} />);
    await user.click(tab("Profile"));
    await user.keyboard("{ArrowLeft}");
    assert.equal(document.activeElement, tab("Alerts"));
    await user.keyboard("{ArrowRight}");
    assert.equal(document.activeElement, tab("Profile"));
  });

  test("Home and End jump to the first and last enabled tabs", async () => {
    const user = userEvent.setup();
    const changes: string[] = [];
    const tabs: readonly Tab[] = [
      { id: "x", title: "Off", content: "x", disabled: true },
      ...TABS.slice(0, 3),
    ];
    render(
      <Tabs
        label="Settings"
        tabs={tabs}
        defaultTabId="security"
        onChange={(id) => changes.push(id)}
      />,
    );
    await user.click(tab("Security"));
    await user.keyboard("{Home}");
    assert.equal(document.activeElement, tab("Profile"));
    await user.keyboard("{End}");
    assert.equal(document.activeElement, tab("Security"));
    assert.deepEqual(changes, ["profile", "security"]);
  });
});

import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { render, screen, within } from "@testing-library/react";
import { Badge } from "../Badge.tsx";
import { TeamList } from "../TeamList.tsx";
import { UserCard, type User } from "../UserCard.tsx";

const ada: User = Object.freeze({
  id: "u1",
  name: "Ada",
  email: "ada@example.com",
  active: true,
  skills: Object.freeze(["math", "engines"]),
});
const linus: User = Object.freeze({ id: "u2", name: "Linus", active: false, skills: [] });
const grace: User = Object.freeze({ id: "u3", name: "Grace", active: true, skills: ["cobol"] });

describe("Badge", () => {
  test("uses the tone in the class name", () => {
    render(<Badge label="Online" tone="success" />);
    assert.equal(screen.getByText("Online").className, "badge badge-success");
  });
});

describe("UserCard", () => {
  test("shows the name as a heading inside an article named after the user", () => {
    render(<UserCard user={ada} />);
    const card = screen.getByRole("article", { name: "Ada" });
    assert.equal(within(card).getByRole("heading", { level: 2 }).textContent, "Ada");
  });

  test("shows an Active or Inactive badge", () => {
    render(
      <>
        <UserCard user={ada} />
        <UserCard user={linus} />
      </>,
    );
    assert.equal(screen.getByText("Active").className, "badge badge-success");
    assert.equal(screen.getByText("Inactive").className, "badge badge-neutral");
  });

  test("links the email only when there is one", () => {
    const { unmount } = render(<UserCard user={ada} />);
    const link = screen.getByRole("link", { name: "ada@example.com" });
    assert.equal(link.getAttribute("href"), "mailto:ada@example.com");
    unmount();
    render(<UserCard user={linus} />);
    assert.equal(screen.queryByRole("link"), null);
  });

  test("lists the skills, or says there are none", () => {
    render(<UserCard user={ada} />);
    const items = screen.getAllByRole("listitem").map((item) => item.textContent);
    assert.deepEqual(items, ["math", "engines"]);
    assert.equal(screen.queryByText("No skills yet"), null);
  });

  test("says when a user has no skills", () => {
    render(<UserCard user={linus} />);
    assert.ok(screen.getByText("No skills yet"));
    assert.equal(screen.queryByRole("list"), null);
  });
});

describe("TeamList", () => {
  test("shows the title and the count of members", () => {
    render(<TeamList title="Core team" users={Object.freeze([ada, linus, grace])} />);
    const section = screen.getByRole("region", { name: "Core team" });
    assert.equal(within(section).getByRole("heading", { level: 1 }).textContent, "Core team");
    assert.ok(within(section).getByText("3 members"));
  });

  test("sorts the cards by name without mutating the props", () => {
    const users = Object.freeze([linus, grace, ada]);
    render(<TeamList title="Team" users={users} />);
    const names = screen.getAllByRole("article").map((card) => card.getAttribute("aria-label"));
    assert.deepEqual(names, ["Ada", "Grace", "Linus"]);
    assert.deepEqual(
      users.map((user) => user.name),
      ["Linus", "Grace", "Ada"],
    );
  });

  test("handles one member and no members", () => {
    render(<TeamList title="Solo" users={[grace]} />);
    assert.ok(screen.getByText("1 member"));
    render(<TeamList title="Empty" users={[]} />);
    const empty = screen.getByRole("region", { name: "Empty" });
    assert.ok(within(empty).getByText("0 members"));
    assert.ok(within(empty).getByText("No members yet"));
  });
});

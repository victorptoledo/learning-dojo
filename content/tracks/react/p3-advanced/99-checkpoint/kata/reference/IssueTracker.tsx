import { memo, useCallback, useId, useMemo, useRef, useState, type FormEvent, type ReactNode } from "react";
import { matchesQuery, openCount, renameIssue, toggleIssue, type Issue, type IssueState } from "./issues.ts";
import { trackRender } from "./renderLog.ts";
import { useStore, type Store } from "./store.ts";

export type IssueTrackerProps = {
  store: Store<IssueState>;
};

function readQuery(): string {
  return new URLSearchParams(window.location.search).get("q") ?? "";
}

function writeQuery(query: string): void {
  const url = new URL(window.location.href);
  if (query === "") {
    url.searchParams.delete("q");
  } else {
    url.searchParams.set("q", query);
  }
  // Replace, not push: every keystroke would otherwise become a Back step.
  window.history.replaceState(window.history.state, "", url.pathname + url.search + url.hash);
}

const selectIssues = (state: IssueState) => state.issues;

/** The filter, the list of issues and the editor of the selected issue (see README.md). */
export function IssueTracker({ store }: IssueTrackerProps): ReactNode {
  const issues = useStore(store, selectIssues);
  const [query, setQuery] = useState(readQuery);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const filterId = useId();

  const toggle = useCallback((id: string) => store.setState((state) => toggleIssue(state, id)), [store]);
  const rename = useCallback(
    (id: string, title: string) => store.setState((state) => renameIssue(state, id, title)),
    [store],
  );
  const select = useCallback((id: string) => setSelectedId(id), []);

  const visible = useMemo(() => issues.filter((issue) => matchesQuery(issue, query)), [issues, query]);
  const selected = issues.find((issue) => issue.id === selectedId) ?? null;

  return (
    <section aria-label="Issue tracker">
      <h1>Issues</h1>
      <label htmlFor={filterId}>Filter</label>
      <input
        id={filterId}
        value={query}
        onChange={(event) => {
          setQuery(event.target.value);
          writeQuery(event.target.value.trim());
        }}
      />
      <p role="status">
        {openCount(issues)} of {issues.length} open
      </p>
      {visible.length === 0 ? (
        <p>No issues match</p>
      ) : (
        <ul aria-label="Issues">
          {visible.map((issue) => (
            <IssueRow
              key={issue.id}
              issue={issue}
              selected={issue.id === selectedId}
              onToggle={toggle}
              onSelect={select}
            />
          ))}
        </ul>
      )}
      {selected ? (
        // A new key per issue: the draft belongs to one issue and starts over for the next.
        <IssueEditor key={selected.id} issue={selected} onRename={rename} />
      ) : (
        <p>Select an issue to edit it</p>
      )}
    </section>
  );
}

type IssueRowProps = {
  issue: Issue;
  selected: boolean;
  onToggle: (id: string) => void;
  onSelect: (id: string) => void;
};

const IssueRow = memo(function IssueRow({ issue, selected, onToggle, onSelect }: IssueRowProps) {
  trackRender(`row:${issue.id}`);
  return (
    <li>
      <input
        type="checkbox"
        aria-label={`Done: ${issue.title}`}
        checked={issue.done}
        onChange={() => onToggle(issue.id)}
      />
      <button type="button" aria-pressed={selected} onClick={() => onSelect(issue.id)}>
        {issue.title}
      </button>
    </li>
  );
});

type IssueEditorProps = {
  issue: Issue;
  onRename: (id: string, title: string) => void;
};

function IssueEditor({ issue, onRename }: IssueEditorProps) {
  const [draft, setDraft] = useState(issue.title);
  const [error, setError] = useState<string | null>(null);
  const input = useRef<HTMLInputElement>(null);
  const titleId = useId();
  const errorId = useId();

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (draft.trim() === "") {
      setError("Title cannot be empty");
      input.current?.focus();
      return;
    }
    setError(null);
    onRename(issue.id, draft);
  }

  return (
    <form aria-label="Edit issue" onSubmit={submit} noValidate>
      <label htmlFor={titleId}>Title</label>
      <input
        id={titleId}
        ref={input}
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
      />
      {error && (
        <p id={errorId} role="alert">
          {error}
        </p>
      )}
      <button type="submit">Save</button>
    </form>
  );
}

import {
  createContext,
  useContext,
  useEffect,
  useId,
  useReducer,
  useRef,
  useState,
  type Dispatch,
  type FormEvent,
  type KeyboardEvent,
  type ReactNode,
} from "react";
import { boardReducer, initialState, type BoardAction, type BoardState } from "./board-state.ts";
import type { BoardApi, BoardData, Card, Column } from "./board-types.ts";

type SaveStatus = "saved" | "saving" | "failed";
type BoardContextValue = {
  state: BoardState;
  dispatch: Dispatch<BoardAction>;
  createId: () => string;
  /** The card whose Move button takes the focus after its next render (after Escape). */
  pendingFocus: { current: string | null };
  save: SaveStatus | null;
  dismissError: () => void;
};

const BoardContext = createContext<BoardContextValue | null>(null);

export function useBoard(): BoardContextValue {
  const value = useContext(BoardContext);
  if (!value) {
    throw new Error("useBoard must be used inside a BoardProvider");
  }
  return value;
}

export type BoardProviderProps = {
  initial: BoardData;
  /** Makes the id of a new card. */
  createId?: () => string;
  /** Saves every change (stage 4). */
  api?: BoardApi;
  children: ReactNode;
};

/** Holds the board for every component under it. */
export function BoardProvider({ initial, createId, api, children }: BoardProviderProps): ReactNode {
  const [state, dispatch] = useReducer(boardReducer, initial, initialState);
  const [save, setSave] = useState<SaveStatus>("saved");
  const saved = useRef(initial);
  const sent = useRef(initial);
  const inFlight = useRef(false);
  const fallbackId = useRef(0);
  const pendingFocus = useRef<string | null>(null);

  useEffect(() => {
    if (!api || inFlight.current || state.carrying || state.board === sent.current) {
      return;
    }
    const board = state.board;
    inFlight.current = true;
    sent.current = board;
    setSave("saving");
    api.save(board).then(
      () => {
        inFlight.current = false;
        saved.current = board;
        // The render this causes runs the effect again, which saves what changed meanwhile.
        setSave("saved");
      },
      () => {
        inFlight.current = false;
        sent.current = saved.current;
        setSave("failed");
        dispatch({ type: "restore", board: saved.current });
      },
    );
  });

  const value: BoardContextValue = {
    state,
    dispatch,
    createId: createId ?? (() => `card-${++fallbackId.current}`),
    pendingFocus,
    save: api ? save : null,
    dismissError: () => setSave("saved"),
  };
  return <BoardContext.Provider value={value}>{children}</BoardContext.Provider>;
}

const INSTRUCTIONS = "Press Space to pick up a card, the arrow keys to move it, Space to drop it and Escape to cancel.";

/** The columns and their cards. */
export function Board(): ReactNode {
  const { state, dispatch, save, dismissError } = useBoard();
  const instructions = useId();
  const busy = state.carrying !== null;

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (!(event.ctrlKey || event.metaKey) || event.target instanceof HTMLInputElement) {
      return;
    }
    const key = event.key.toLowerCase();
    if (key === "z" && !event.shiftKey) {
      event.preventDefault();
      dispatch({ type: "undo" });
    } else if (key === "y" || (key === "z" && event.shiftKey)) {
      event.preventDefault();
      dispatch({ type: "redo" });
    }
  }

  return (
    <div onKeyDown={onKeyDown}>
      <div role="toolbar" aria-label="History">
        <button type="button" disabled={busy || state.past.length === 0} onClick={() => dispatch({ type: "undo" })}>
          Undo
        </button>
        <button type="button" disabled={busy || state.future.length === 0} onClick={() => dispatch({ type: "redo" })}>
          Redo
        </button>
      </div>
      {save === "saving" ? <p>Saving…</p> : null}
      {save === "saved" ? <p>All changes saved</p> : null}
      {save === "failed" ? (
        <div role="alert">
          <p>Could not save. The board was restored to the last saved version.</p>
          <button type="button" onClick={dismissError}>
            Dismiss
          </button>
        </div>
      ) : null}
      <p id={instructions} hidden>
        {INSTRUCTIONS}
      </p>
      <p role="status">{state.announcement}</p>
      <div>
        {state.board.columns.map((column) => (
          <ColumnView key={column.id} column={column} instructions={instructions} />
        ))}
      </div>
    </div>
  );
}

function ColumnView({ column, instructions }: { column: Column; instructions: string }): ReactNode {
  const { dispatch, createId } = useBoard();
  const [title, setTitle] = useState("");
  const heading = useId();
  const input = useRef<HTMLInputElement>(null);

  function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const text = title.trim();
    if (text !== "") {
      dispatch({ type: "add", columnId: column.id, card: { id: createId(), title: text } });
      setTitle("");
    }
    input.current?.focus();
  }

  return (
    <section aria-labelledby={heading}>
      <h2 id={heading}>{column.title}</h2>
      {column.cards.length === 0 ? (
        <p>No cards</p>
      ) : (
        <ul>
          {column.cards.map((card) => (
            <CardView key={card.id} card={card} instructions={instructions} onDeleted={() => input.current?.focus()} />
          ))}
        </ul>
      )}
      <form aria-label={`Add a card to ${column.title}`} onSubmit={add}>
        <label>
          {`New card in ${column.title}`}
          <input ref={input} value={title} onChange={(event) => setTitle(event.target.value)} />
        </label>
        <button type="submit">Add card</button>
      </form>
    </section>
  );
}

function CardView(props: { card: Card; instructions: string; onDeleted: () => void }): ReactNode {
  const { card, instructions, onDeleted } = props;
  const { state, dispatch, pendingFocus } = useBoard();
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(card.title);
  const editButton = useRef<HTMLButtonElement>(null);
  const focusEdit = useRef(false);
  const carried = state.carrying?.cardId === card.id;

  useEffect(() => {
    if (!editing && focusEdit.current) {
      focusEdit.current = false;
      editButton.current?.focus();
    }
  }, [editing]);

  function finish(title: string | null) {
    if (title !== null && title.trim() !== "") {
      dispatch({ type: "rename", cardId: card.id, title: title.trim() });
    }
    focusEdit.current = true;
    setEditing(false);
  }

  function onMoveKey(event: KeyboardEvent<HTMLButtonElement>) {
    if (!carried) {
      return;
    }
    const moves: Record<string, [number, number]> = {
      ArrowUp: [0, -1],
      ArrowDown: [0, 1],
      ArrowLeft: [-1, 0],
      ArrowRight: [1, 0],
    };
    const move = moves[event.key];
    if (move) {
      event.preventDefault();
      dispatch({ type: "move", dx: move[0], dy: move[1] });
    } else if (event.key === "Escape") {
      event.preventDefault();
      pendingFocus.current = card.id;
      dispatch({ type: "cancel" });
    }
  }

  if (editing) {
    return (
      <li>
        <input
          aria-label={`Title of ${card.title}`}
          value={draft}
          autoFocus
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault();
              finish(draft);
            } else if (event.key === "Escape") {
              event.preventDefault();
              finish(null);
            }
          }}
        />
        <button type="button" onClick={() => finish(draft)}>
          Save
        </button>
        <button type="button" onClick={() => finish(null)}>
          Cancel
        </button>
      </li>
    );
  }

  return (
    <li>
      <span>{card.title}</span>{" "}
      <button
        type="button"
        aria-pressed={carried}
        aria-describedby={instructions}
        ref={(element) => {
          if (element && (carried || pendingFocus.current === card.id)) {
            pendingFocus.current = null;
            if (document.activeElement !== element) {
              element.focus();
            }
          }
        }}
        onClick={() => dispatch(carried ? { type: "drop" } : { type: "pick", cardId: card.id })}
        onKeyDown={onMoveKey}
      >
        {`Move ${card.title}`}
      </button>{" "}
      <button
        type="button"
        ref={editButton}
        onClick={() => {
          setDraft(card.title);
          setEditing(true);
        }}
      >
        {`Edit ${card.title}`}
      </button>{" "}
      <button
        type="button"
        onClick={() => {
          dispatch({ type: "delete", cardId: card.id });
          onDeleted();
        }}
      >
        {`Delete ${card.title}`}
      </button>
    </li>
  );
}

/** How many cards each column has; it can be rendered anywhere under the provider. */
export function BoardSummary(): ReactNode {
  const { state } = useBoard();
  const total = state.board.columns.reduce((sum, column) => sum + column.cards.length, 0);
  return (
    <section aria-label="Summary">
      <ul>
        {state.board.columns.map((column) => (
          <li key={column.id}>{`${column.title}: ${column.cards.length}`}</li>
        ))}
      </ul>
      <p>{`${total} ${total === 1 ? "card" : "cards"} in total`}</p>
    </section>
  );
}

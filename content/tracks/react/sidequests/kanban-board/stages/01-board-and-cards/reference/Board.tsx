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
  type ReactNode,
} from "react";
import { boardReducer, initialState, type BoardAction, type BoardState } from "./board-state.ts";
import type { BoardApi, BoardData, Card, Column } from "./board-types.ts";

type BoardContextValue = {
  state: BoardState;
  dispatch: Dispatch<BoardAction>;
  createId: () => string;
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
export function BoardProvider({ initial, createId, children }: BoardProviderProps): ReactNode {
  const [state, dispatch] = useReducer(boardReducer, initial, initialState);
  const fallbackId = useRef(0);
  const value: BoardContextValue = {
    state,
    dispatch,
    createId: createId ?? (() => `card-${++fallbackId.current}`),
  };
  return <BoardContext.Provider value={value}>{children}</BoardContext.Provider>;
}

/** The columns and their cards. */
export function Board(): ReactNode {
  const { state } = useBoard();
  return (
    <div>
      {state.board.columns.map((column) => (
        <ColumnView key={column.id} column={column} />
      ))}
    </div>
  );
}

function ColumnView({ column }: { column: Column }): ReactNode {
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
            <CardView key={card.id} card={card} onDeleted={() => input.current?.focus()} />
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

function CardView({ card, onDeleted }: { card: Card; onDeleted: () => void }): ReactNode {
  const { dispatch } = useBoard();
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(card.title);
  const editButton = useRef<HTMLButtonElement>(null);
  const focusEdit = useRef(false);

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

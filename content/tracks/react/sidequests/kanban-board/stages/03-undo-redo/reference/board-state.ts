// The board's state and its pure reducer: every change goes through `boardReducer`.
import type { BoardData, Card, Column } from "./board-types.ts";

export const HISTORY_LIMIT = 20;

type Step = { board: BoardData; label: string };
type Carrying = { cardId: string; before: BoardData };

export type BoardState = {
  board: BoardData;
  past: readonly Step[];
  future: readonly Step[];
  carrying: Carrying | null;
  announcement: string;
};

export type BoardAction =
  | { type: "add"; columnId: string; card: Card }
  | { type: "delete"; cardId: string }
  | { type: "rename"; cardId: string; title: string }
  | { type: "pick"; cardId: string }
  | { type: "move"; dx: number; dy: number }
  | { type: "drop" }
  | { type: "cancel" }
  | { type: "undo" }
  | { type: "redo" };

export type Place = { column: Column; columnIndex: number; index: number; card: Card };

export function initialState(board: BoardData): BoardState {
  return { board, past: [], future: [], carrying: null, announcement: "" };
}

export function find(board: BoardData, cardId: string): Place | null {
  for (const [columnIndex, column] of board.columns.entries()) {
    const index = column.cards.findIndex((card) => card.id === cardId);
    const card = column.cards[index];
    if (card) {
      return { column, columnIndex, index, card };
    }
  }
  return null;
}

function where(board: BoardData, cardId: string): string {
  const place = find(board, cardId);
  if (!place) {
    return "";
  }
  return `${place.column.title}, position ${place.index + 1} of ${place.column.cards.length}`;
}

function withCards(board: BoardData, columnIndex: number, cards: readonly Card[]): BoardData {
  return { columns: board.columns.map((column, index) => (index === columnIndex ? { ...column, cards } : column)) };
}

function commit(state: BoardState, board: BoardData, label: string, announcement = state.announcement): BoardState {
  return {
    ...state,
    board,
    past: [...state.past, { board: state.board, label }].slice(-HISTORY_LIMIT),
    future: [],
    announcement,
  };
}

function moved(board: BoardData, cardId: string, dx: number, dy: number): BoardData {
  const place = find(board, cardId);
  if (!place) {
    return board;
  }
  const without = place.column.cards.filter((card) => card.id !== cardId);
  if (dx === 0) {
    const index = place.index + dy;
    if (index < 0 || index > without.length) {
      return board;
    }
    return withCards(board, place.columnIndex, [...without.slice(0, index), place.card, ...without.slice(index)]);
  }
  const target = place.columnIndex + dx;
  const column = board.columns[target];
  if (!column) {
    return board;
  }
  const index = Math.min(place.index, column.cards.length);
  const cards = [...column.cards.slice(0, index), place.card, ...column.cards.slice(index)];
  return withCards(withCards(board, place.columnIndex, without), target, cards);
}

export function boardReducer(state: BoardState, action: BoardAction): BoardState {
  if (state.carrying && !["move", "drop", "cancel"].includes(action.type)) {
    return state;
  }
  switch (action.type) {
    case "add": {
      const columnIndex = state.board.columns.findIndex((column) => column.id === action.columnId);
      const column = state.board.columns[columnIndex];
      if (!column) {
        return state;
      }
      const board = withCards(state.board, columnIndex, [...column.cards, action.card]);
      return commit(state, board, `adding ${action.card.title}`);
    }
    case "delete": {
      const place = find(state.board, action.cardId);
      if (!place) {
        return state;
      }
      const cards = place.column.cards.filter((card) => card.id !== action.cardId);
      return commit(state, withCards(state.board, place.columnIndex, cards), `deleting ${place.card.title}`);
    }
    case "rename": {
      const place = find(state.board, action.cardId);
      if (!place || place.card.title === action.title) {
        return state;
      }
      const cards = place.column.cards.map((card) => (card.id === action.cardId ? { ...card, title: action.title } : card));
      return commit(
        state,
        withCards(state.board, place.columnIndex, cards),
        `renaming ${place.card.title} to ${action.title}`,
      );
    }
    case "pick": {
      const place = find(state.board, action.cardId);
      if (!place) {
        return state;
      }
      return {
        ...state,
        carrying: { cardId: action.cardId, before: state.board },
        announcement: `Picked up ${place.card.title} in ${where(state.board, action.cardId)}.`,
      };
    }
    case "move": {
      if (!state.carrying) {
        return state;
      }
      const board = moved(state.board, state.carrying.cardId, action.dx, action.dy);
      if (board === state.board) {
        return state;
      }
      const title = find(board, state.carrying.cardId)?.card.title ?? "";
      return { ...state, board, announcement: `${title} is in ${where(board, state.carrying.cardId)}.` };
    }
    case "drop": {
      const carrying = state.carrying;
      if (!carrying) {
        return state;
      }
      const title = find(state.board, carrying.cardId)?.card.title ?? "";
      const announcement = `Dropped ${title} in ${where(state.board, carrying.cardId)}.`;
      const stayed = where(state.board, carrying.cardId) === where(carrying.before, carrying.cardId);
      if (stayed) {
        return { ...state, board: carrying.before, carrying: null, announcement };
      }
      return { ...commit({ ...state, board: carrying.before }, state.board, `moving ${title}`, announcement), carrying: null };
    }
    case "cancel": {
      const carrying = state.carrying;
      if (!carrying) {
        return state;
      }
      const title = find(carrying.before, carrying.cardId)?.card.title ?? "";
      return {
        ...state,
        board: carrying.before,
        carrying: null,
        announcement: `Cancelled. ${title} is back in ${where(carrying.before, carrying.cardId)}.`,
      };
    }
    case "undo": {
      const step = state.past.at(-1);
      if (!step) {
        return state;
      }
      return {
        ...state,
        board: step.board,
        past: state.past.slice(0, -1),
        future: [...state.future, { board: state.board, label: step.label }],
        announcement: `Undid ${step.label}.`,
      };
    }
    case "redo": {
      const step = state.future.at(-1);
      if (!step) {
        return state;
      }
      return {
        ...state,
        board: step.board,
        past: [...state.past, { board: state.board, label: step.label }],
        future: state.future.slice(0, -1),
        announcement: `Redid ${step.label}.`,
      };
    }
  }
}

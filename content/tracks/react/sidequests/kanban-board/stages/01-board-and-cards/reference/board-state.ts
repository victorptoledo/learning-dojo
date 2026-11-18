// The board's state and its pure reducer: every change goes through `boardReducer`.
import type { BoardData, Card } from "./board-types.ts";

export type BoardState = { board: BoardData };

export type BoardAction =
  | { type: "add"; columnId: string; card: Card }
  | { type: "delete"; cardId: string }
  | { type: "rename"; cardId: string; title: string };

export function initialState(board: BoardData): BoardState {
  return { board };
}

function mapCards(board: BoardData, change: (cards: readonly Card[], columnId: string) => readonly Card[]): BoardData {
  return { columns: board.columns.map((column) => ({ ...column, cards: change(column.cards, column.id) })) };
}

export function boardReducer(state: BoardState, action: BoardAction): BoardState {
  switch (action.type) {
    case "add":
      return {
        board: mapCards(state.board, (cards, columnId) =>
          columnId === action.columnId ? [...cards, action.card] : cards,
        ),
      };
    case "delete":
      return { board: mapCards(state.board, (cards) => cards.filter((card) => card.id !== action.cardId)) };
    case "rename":
      return {
        board: mapCards(state.board, (cards) =>
          cards.map((card) => (card.id === action.cardId ? { ...card, title: action.title } : card)),
        ),
      };
  }
}

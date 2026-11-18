// The data the board shows and saves. The tests build boards of these types, so keep them as
// they are; how you hold them in state is yours.

export type Card = { id: string; title: string };

export type Column = { id: string; title: string; cards: readonly Card[] };

export type BoardData = { columns: readonly Column[] };

/** Used from stage 4. Rejects when the board could not be saved. */
export type BoardApi = { save(board: BoardData): Promise<void> };

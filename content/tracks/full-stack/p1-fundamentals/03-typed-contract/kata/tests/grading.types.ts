import { createTodo, type CreateResult } from "../client.ts";
import type { NewTodoInput, Problem, Todo } from "../contract.ts";
import type { Equal, Expect } from "./expect.ts";

// test: Todo is derived from the schema
export type TodoShape = Expect<
  Equal<Todo, { id: number; title: string; due: string | null; done: boolean }>
>;

// test: NewTodoInput has a required title and an optional due date
export type InputShape = Expect<Equal<NewTodoInput, { title: string; due?: string | undefined }>>;
// @ts-expect-error: a title is required
export const noTitle: NewTodoInput = { due: "2026-01-01" };

// test: the result of createTodo narrows on ok
declare const result: CreateResult;
export const narrowed: string = result.ok ? result.todo.title : result.problem.title;
// @ts-expect-error: a failed result has no todo
export const wrong: Todo = result.ok ? result.todo : result.todo;
export type ProblemErrors = Expect<Equal<Problem["errors"], { path: string; message: string }[] | undefined>>;
// @ts-expect-error: the title must be a string
export const call = createTodo("http://x", { title: 5 });

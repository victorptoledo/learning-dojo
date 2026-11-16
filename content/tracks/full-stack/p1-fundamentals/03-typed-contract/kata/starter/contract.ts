// One contract for both sides: the server validates requests with it, the client validates
// answers with it, and every type below is derived from a schema.
import { z } from "zod";

// todo: trim the title and keep it 1 to 120 characters; due must be a real ISO date
// (YYYY-MM-DD); refuse unknown keys.
export const NewTodo = z.object({
  title: z.string(),
  due: z.string().optional(),
});

// todo: id a positive integer, due an ISO date or null.
export const Todo = z.object({
  id: z.number(),
  title: z.string(),
  due: z.string().nullable(),
  done: z.boolean(),
});

export const FieldError = z.object({ path: z.string(), message: z.string() });

// todo: status must be an integer.
export const Problem = z.object({
  type: z.string(),
  title: z.string(),
  status: z.number(),
  errors: z.array(FieldError).optional(),
});

/** What a client may send (before trimming). */
export type NewTodoInput = z.input<typeof NewTodo>;
export type Todo = z.infer<typeof Todo>;
export type FieldError = z.infer<typeof FieldError>;
export type Problem = z.infer<typeof Problem>;

/** One { path, message } per issue; the path joined with "." ("" for the whole object). */
export function toFieldErrors(error: z.ZodError): FieldError[] {
  throw new Error("todo: write toFieldErrors()");
}

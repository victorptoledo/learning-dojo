// One contract for both sides: the server validates requests with it, the client validates
// answers with it, and every type below is derived from a schema.
import { z } from "zod";

export const NewTodo = z.strictObject({
  title: z.string().trim().min(1).max(120),
  due: z.iso.date().optional(),
});

export const Todo = z.object({
  id: z.number().int().positive(),
  title: z.string(),
  due: z.iso.date().nullable(),
  done: z.boolean(),
});

export const FieldError = z.object({ path: z.string(), message: z.string() });

export const Problem = z.object({
  type: z.string(),
  title: z.string(),
  status: z.number().int(),
  errors: z.array(FieldError).optional(),
});

/** What a client may send (before trimming). */
export type NewTodoInput = z.input<typeof NewTodo>;
export type Todo = z.infer<typeof Todo>;
export type FieldError = z.infer<typeof FieldError>;
export type Problem = z.infer<typeof Problem>;

/** One { path, message } per issue; the path joined with "." ("" for the whole object). */
export function toFieldErrors(error: z.ZodError): FieldError[] {
  return error.issues.map((issue) => ({
    path: issue.path.map(String).join("."),
    message: issue.message,
  }));
}

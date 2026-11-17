import { createApiClient, defineApi, type HttpError, type PathParams, type Result } from "../client.ts";
import type { Equal, Expect } from "./expect.ts";

type User = { id: number; name: string };
type NewUser = { name: string };
declare function parseUser(value: unknown): User;

const api = defineApi({
  listUsers: { method: "GET", path: "/users", decode: (json: unknown) => [json] },
  getUser: { method: "GET", path: "/users/:id", decode: parseUser },
  createUser: { method: "POST", path: "/users", encode: (user: NewUser) => user, decode: parseUser },
  getPost: { method: "GET", path: "/users/:userId/posts/:postId", decode: (json: unknown) => String(json) },
});
const users = createApiClient(api, { baseUrl: "http://localhost" });

// test: PathParams has one key per parameter
export type TwoKeys = Expect<Equal<keyof PathParams<"/users/:userId/posts/:postId">, "userId" | "postId">>;
export type OneKey = Expect<Equal<keyof PathParams<"/files/:name">, "name">>;
export type NoKeys = Expect<Equal<keyof PathParams<"/users">, never>>;
export const both: PathParams<"/a/:x/b/:y"> = { x: "1", y: "2" };

// test: PathParams values are text and all required
// @ts-expect-error: y is missing
export const onlyX: PathParams<"/a/:x/b/:y"> = { x: "1" };
// @ts-expect-error: parameters are strings
export const numeric: PathParams<"/a/:x"> = { x: 1 };

// test: defineApi keeps literal paths and methods
export type Path = Expect<Equal<(typeof api)["getUser"]["path"], "/users/:id">>;
export type Method = Expect<Equal<(typeof api)["createUser"]["method"], "POST">>;

// test: each method returns a Result of what decode returns
export type GetUser = Expect<Equal<ReturnType<typeof users.getUser>, Promise<Result<User, HttpError>>>>;
export type GetPost = Expect<Equal<ReturnType<typeof users.getPost>, Promise<Result<string, HttpError>>>>;
export type List = Expect<Equal<ReturnType<typeof users.listUsers>, Promise<Result<unknown[], HttpError>>>>;

// test: a call with nothing required can skip its argument
users.listUsers();
users.listUsers({ query: { page: 1 } });

// test: path parameters are required and checked
users.getUser({ params: { id: "1" } });
users.getPost({ params: { userId: "1", postId: "2" } });
// @ts-expect-error: params is required
users.getUser();
// @ts-expect-error: params is required
users.getUser({});
// @ts-expect-error: "uid" is not the parameter's name
users.getUser({ params: { uid: "1" } });
// @ts-expect-error: postId is missing
users.getPost({ params: { userId: "1" } });

// test: params only where the path has parameters
// @ts-expect-error: /users has no parameters
users.listUsers({ params: { id: "1" } });

// test: the body has the type of encode's parameter
users.createUser({ body: { name: "Ana" } });
// @ts-expect-error: body is required
users.createUser();
// @ts-expect-error: name must be a string
users.createUser({ body: { name: 1 } });

// test: no body for endpoints without encode
// @ts-expect-error: getUser takes no body
users.getUser({ params: { id: "1" }, body: { name: "Ana" } });

// test: only the endpoints of the api exist
// @ts-expect-error: deleteUser is not an endpoint
users.deleteUser();

// test: an endpoint needs a method of the README
// @ts-expect-error: "FETCH" is not a method
defineApi({ bad: { method: "FETCH", path: "/", decode: (json: unknown) => json } });

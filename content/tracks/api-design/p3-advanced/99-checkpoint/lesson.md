# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a partner exports API written in a hurry. The
   key travels in the URL, any partner can read any export, nothing limits a client, and each
   export runs inside the request. Move the key to `Authorization: Bearer`, add scopes and
   object-level authorisation, give each partner a token bucket that reports its budget, run
   exports as long-running operations, announce their end with signed webhooks retried with
   backoff, and finish the OpenAPI contract so it says all of that.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| API Keys, OAuth 2.x and Scopes | Where does the key travel? `401` or `403`, and which `WWW-Authenticate`? Whose object is this? |
| Rate Limiting and Quotas | Which key do I limit by? What is the burst, and what is the sustained rate? What do `RateLimit` and `Retry-After` say? |
| OpenAPI as a Contract | Does every operation say its security, its errors and its headers? Where do webhooks go? |
| gRPC, GraphQL and REST | What breaks an old client of a `.proto`? What does a GraphQL query cost? |
| Webhooks and Asynchronous APIs | When is `202` right? What do I sign, and how does a receiver deduplicate a retry? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.

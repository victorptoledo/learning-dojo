# OpenAPI Linter

A style guide that only lives in a wiki is a suggestion; one that runs in CI is a contract.
In this kata you write the linter yourself: read an OpenAPI 3.x document, walk its paths,
operations, responses and schemas, and report each violation as a rule id, a JSON Pointer and
a message, the way Spectral and Redocly report them. Thirteen checks, one per line of the
guide, from version and ownership to verbs in URLs, missing path parameters, security and
problem details. Press **Enter** to create the kata folder and open it.

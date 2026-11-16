# Problem Responses

Errors are part of the contract, and usually the part clients get wrong first. In this kata a
bank API that answers `200 {"error": "..."}` for everything learns to say `404`, `409`, `422`,
`400` and `500` and to put the details in RFC 9457 problem documents: a `type` a client can
switch on, a `title` and `detail` a human can read, extension members a program can use, and
nothing that leaks the server's internals. Press **Enter** to create the kata folder and open it.

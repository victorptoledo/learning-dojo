# Hello, Dojo

Write two functions in `greet.go`.

| Function | Returns |
| --- | --- |
| `Greet(name string, shout bool) string` | `"Welcome to the dojo, <name>!"`. Spaces around the name are removed, and a blank name (empty or only spaces) becomes `stranger`. When `shout` is true, the whole message is in upper case. |
| `Initials(name string) string` | The first letter of each word, in upper case, joined: `"ada lovelace"` gives `"AL"`. Words are separated by any amount of white space. Letters outside ASCII count too: `"élodie durand"` gives `"ÉD"`. A blank name gives `""`. |

```go
greet.Greet("Ada", false)        // "Welcome to the dojo, Ada!"
greet.Greet("   ", false)        // "Welcome to the dojo, stranger!"
greet.Greet(" Grace ", true)     // "WELCOME TO THE DOJO, GRACE!"
greet.Initials("  ada   lovelace ") // "AL"
```

Tools: `strings.TrimSpace`, `strings.ToUpper`, `strings.Fields`, `fmt.Sprintf`,
`strings.Builder`, and for the first letter of a word `utf8.DecodeRuneInString` with
`unicode.ToUpper` (a string is bytes; `word[0]` would cut `é` in half).

Run the visible test with `go test` (or `go test -race`). The dojo runs 7 tests: 1 in
`greet_test.go` and 6 hidden ones, plus `go vet` and `gofmt` on `greet.go`.

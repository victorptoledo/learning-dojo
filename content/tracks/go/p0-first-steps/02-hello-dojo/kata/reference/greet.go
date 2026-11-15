// Package greet welcomes visitors to the dojo.
package greet

import (
	"fmt"
	"strings"
	"unicode"
	"unicode/utf8"
)

// Greet returns "Welcome to the dojo, <name>!". Spaces around name are removed and a blank
// name becomes "stranger". With shout, the whole message is in upper case.
func Greet(name string, shout bool) string {
	who := strings.TrimSpace(name)
	if who == "" {
		who = "stranger"
	}
	message := fmt.Sprintf("Welcome to the dojo, %s!", who)
	if shout {
		return strings.ToUpper(message)
	}
	return message
}

// Initials returns the upper case first letter of each word of name: "ada lovelace" -> "AL".
func Initials(name string) string {
	var initials strings.Builder
	for _, word := range strings.Fields(name) {
		first, _ := utf8.DecodeRuneInString(word)
		initials.WriteRune(unicode.ToUpper(first))
	}
	return initials.String()
}

// Package proxy is a reverse proxy with rate limiting. See README.md.
package proxy

import (
	"errors"
	"net/http"
)

// Config grows a few fields each stage.
type Config struct {
	Upstream string
}

// New builds the proxy's handler.
func New(cfg Config) (http.Handler, error) {
	return nil, errors.New("todo: build the proxy")
}

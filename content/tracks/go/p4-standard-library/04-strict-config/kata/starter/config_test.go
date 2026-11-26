package config_test

import (
	"strings"
	"testing"
	"time"

	"dojo/config"
)

func TestLoadReadsAConfig(t *testing.T) {
	c, err := config.Load(strings.NewReader(`{"name": "billing", "listen": ":8080", "timeout": "45s", "retries": 2}`))
	if err != nil {
		t.Fatalf("Load = %v, want nil", err)
	}
	if c.Name != "billing" || c.Listen != ":8080" {
		t.Errorf("Name, Listen = %q, %q, want billing, :8080", c.Name, c.Listen)
	}
	if time.Duration(c.Timeout) != 45*time.Second {
		t.Errorf("Timeout = %v, want 45s", time.Duration(c.Timeout))
	}
	if c.Retries == nil || *c.Retries != 2 {
		t.Errorf("Retries = %v, want a pointer to 2", c.Retries)
	}
}

// Package stock keeps the inventory in a JSON file, rewritten atomically on every change.
package stock

import (
	"encoding/json"
	"errors"
	"io"
	"io/fs"
	"os"
	"path/filepath"
	"sort"
	"sync"
)

// Item is the quantity in stock of one SKU.
type Item struct {
	SKU string `json:"sku"`
	Qty int    `json:"qty"`
}

// Store is the inventory saved in <dir>/stock.json.
type Store struct {
	mu    sync.Mutex
	file  string
	items map[string]int
}

// Open loads the store and checks that the folder is writable.
func Open(dir string) (*Store, error) {
	if err := os.MkdirAll(dir, 0o750); err != nil {
		return nil, err
	}
	store := &Store{file: filepath.Join(dir, "stock.json"), items: map[string]int{}}
	data, err := os.ReadFile(store.file)
	switch {
	case errors.Is(err, fs.ErrNotExist):
	case err != nil:
		return nil, err
	default:
		var items []Item
		if err := json.Unmarshal(data, &items); err != nil {
			return nil, err
		}
		for _, item := range items {
			store.items[item.SKU] = item.Qty
		}
	}
	return store, store.save()
}

// Set changes the quantity of a SKU and saves the store.
func (s *Store) Set(sku string, qty int) (Item, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.items[sku] = qty
	return Item{SKU: sku, Qty: qty}, s.saveLocked()
}

// WriteJSON writes every item, sorted by SKU.
func (s *Store) WriteJSON(w io.Writer) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	return json.NewEncoder(w).Encode(s.sorted())
}

func (s *Store) save() error {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.saveLocked()
}

func (s *Store) saveLocked() error {
	data, err := json.Marshal(s.sorted())
	if err != nil {
		return err
	}
	tmp := s.file + ".tmp"
	if err := os.WriteFile(tmp, data, 0o640); err != nil {
		return err
	}
	return os.Rename(tmp, s.file)
}

func (s *Store) sorted() []Item {
	items := make([]Item, 0, len(s.items))
	for sku, qty := range s.items {
		items = append(items, Item{SKU: sku, Qty: qty})
	}
	sort.Slice(items, func(i, j int) bool { return items[i].SKU < items[j].SKU })
	return items
}

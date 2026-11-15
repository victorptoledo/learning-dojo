// Package warm imports the packages katas use, so building the image caches their vet facts.
package warm

import (
	"bufio"
	"cmp"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"iter"
	"log/slog"
	"maps"
	"net/http"
	"os"
	"slices"
	"sort"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"
	"unicode"
)

var (
	_ = bufio.NewScanner
	_ = cmp.Compare[int]
	_ = context.Background
	_ = json.Marshal
	_ = errors.New
	_ = fmt.Sprintf
	_ = io.EOF
	_ iter.Seq[int]
	_ = slog.Info
	_ = maps.Keys[map[int]int]
	_ = http.NewServeMux
	_ = os.Getenv
	_ = slices.Sort[[]int]
	_ = sort.Strings
	_ = strconv.Itoa
	_ = strings.Fields
	_ sync.Mutex
	_ atomic.Int64
	_ = time.Now
	_ = unicode.IsLetter
)

# Context Budget

A chat assistant resends the whole conversation on every request, and the model's **context
window** must hold the system prompt, the history and the answer. Write `budget.py`: count
tokens, keep the newest messages that fit, and price requests.

`tokenizer.py` is given (do not edit it). Its `count_tokens(text)` splits text into words and
punctuation and cuts long words into pieces of 6 characters, a simple stand-in for a real BPE
tokenizer:

```python
count_tokens("Hello, world!")   # 4: "Hello" "," "world" "!"
count_tokens("Tokenization")    # 2: "Tokeni" "zation"
```

## Counting

- `MESSAGE_OVERHEAD = 4` (given): every message, the system prompt included, costs 4 tokens of
  formatting on top of its text.
- `Message(role, content)` (given) is a frozen dataclass; `role` is `"user"` or `"assistant"`.
- `message_tokens(message)`: `count_tokens(message.content) + MESSAGE_OVERHEAD`.
- `request_tokens(system, messages)`: the input of one request, the system prompt (its tokens plus
  the overhead) plus every message.

## `fit_history(system, messages, window, reserve)`

Returns the messages to send: the **newest** messages whose tokens, added to the system prompt's,
leave at least `reserve` tokens of the `window` for the answer.

- Walk from the newest message backwards and keep messages while they fit. Stop at the first one
  that does not fit: never skip a message to squeeze in an older one, or the history would have
  holes.
- The result keeps the original order.
- A history must not start with an answer: drop `"assistant"` messages at the start of the result.
- No messages: return `[]`.
- If not even the newest message fits, raise `ContextOverflowError` (given), with the tokens the
  newest message `needed` and the tokens that were `available` for messages (never negative).
- `reserve` below 0, or not below `window`, raises `ValueError`.

## Prices

`Price(input_per_million, output_per_million)` (given) holds `Decimal` prices per million tokens.

- `estimate_cost(input_tokens, output_tokens, price)`: input tokens times the input price plus
  output tokens times the output price, divided by 1,000,000, as a `Decimal` rounded half up to 6
  decimal places (`Decimal("0.000001")`). Negative token counts raise `ValueError`.
- `conversation_cost(system, turns, price)`: `turns` is a list of `(user_text, assistant_text)`
  pairs. Each turn is one request that sends the system prompt, **all earlier turns** (as user and
  assistant messages) and the new user message; its output is the assistant text
  (`count_tokens`, no overhead). Return the sum of the turns' `estimate_cost`s (`Decimal("0")`
  with no turns).

```python
price = Price(Decimal("3"), Decimal("15"))
estimate_cost(1_000, 500, price)      # Decimal("0.010500")
```

Run the visible tests with `uv run pytest`. There are 19 tests: 2 in `tests/test_visible.py`
and 17 hidden ones, plus ruff on each module.

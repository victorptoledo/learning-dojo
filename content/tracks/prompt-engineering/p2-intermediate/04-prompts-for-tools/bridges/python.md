---
from: python
status: draft
concept: "Functions with signatures the model can read"
from_way: "Plain Python functions with type hints and docstrings, called directly by your own code."
native_way: "Each function is offered as a tool: its name, a docstring-like description and a JSON Schema of its parameters; the model asks for a call, your code validates the arguments and calls the function."
sensei_tip: "A dispatcher is a dict from tool name to (schema, function); validate the arguments against the schema, then call function(**arguments). Never pass the model's arguments to eval or a shell."
---

| Python | Tool-use concept |
| --- | --- |
| `def get_weather(city: str, units: Literal["c", "f"])` | a tool with an enum parameter |
| the docstring | the tool description |
| `registry = {"get_weather": (schema, get_weather)}` | a dispatcher |
| `fn(**arguments)` after validation | executing a call |
| `except Exception as error: return str(error)` | an error result |

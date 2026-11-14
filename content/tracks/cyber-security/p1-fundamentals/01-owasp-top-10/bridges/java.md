---
from: java
status: draft
concept: "Defensive posture"
from_way: "Blindly trusting the framework to sanitise all user input."
native_way: "Strict validation at the edges, prepared statements everywhere and least-privilege policies."
sensei_tip: "SSRF is one of the most critical cloud-era attacks: never let your application fetch user-supplied URLs without strict validation of the target."
---

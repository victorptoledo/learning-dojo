---
from: java
status: draft
concept: "Architecture is cost: measure it, attribute it, optimise it"
from_way: "Servers bought for the peak, paid once, and nobody looks at their utilisation."
native_way: "Pay per use, commit only to the baseline, tag everything, and watch unit cost."
sensei_tip: "JVM services often over-ask for memory: measure heap and RSS, then right-size tasks and try Graviton; Corretto on arm64 usually cuts cost per request noticeably."
---

| Java world | AWS |
| --- | --- |
| a capacity plan for the year | auto scaling plus Savings Plans for the baseline |
| a fixed licence budget | AWS Budgets and anomaly alerts |
| `-Xmx8g` everywhere | right-sized tasks and instances |
| project cost codes | cost allocation tags |

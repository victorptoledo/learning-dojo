---
from: java
status: draft
concept: "Data processing"
from_way: "Processing data row by row in imperative batch code."
native_way: "Load raw data straight into the warehouse (EL) and transform it with the warehouse's own compute (T, e.g. with dbt)."
sensei_tip: "Fact tables hold numeric measures and historical events; dimension tables hold descriptive context (who, when, where)."
---

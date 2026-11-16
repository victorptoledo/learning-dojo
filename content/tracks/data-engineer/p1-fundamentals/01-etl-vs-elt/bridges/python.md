---
from: python
status: draft
concept: "Where transformation runs"
from_way: "pandas reads the source, cleans and merges DataFrames in memory, then writes the result."
native_way: "Land the raw data first, then express the cleaning and joins as SQL models the warehouse runs."
sensei_tip: "pandas transformations are ETL on one machine. When the data outgrows memory, ELT moves the same logic into SQL that runs where the data is."
---

| Python | ETL / ELT |
| --- | --- |
| pd.read_sql / requests.get | extract |
| df.merge, df.groupby | the T of ETL |
| df.to_sql('final') | load of ETL |
| df.to_sql('raw_...') then SQL views | ELT |
| a dbt model | a SELECT saved as a table or view |

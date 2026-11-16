# Chunks and a Vector Index

Retrieval starts long before the query: documents are cut into chunks small enough to embed and
to fit a prompt, and the vectors go into an index. You write both halves, an exact index that a
vector database would approximate at scale, plus the rank fusion behind hybrid search. Press
**Enter** to create the kata folder and open it.

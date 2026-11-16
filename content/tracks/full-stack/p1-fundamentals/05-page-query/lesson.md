# From Tables to a Page: the "My Orders" Screen

Replace a screen that made one query per order (N+1) with a single PostgreSQL query that returns
the whole page as the JSON the UI renders, with keyset pagination, a next cursor and the
migration the screen needs. Press **Enter** to create the kata folder and open it.

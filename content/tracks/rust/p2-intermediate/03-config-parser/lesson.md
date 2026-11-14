# Config Parser: Errors as Values

Rust has no exceptions: a function that can fail returns `Result<T, E>`. A good error
type is an enum with one variant per failure, carrying the data needed to explain it,
plus `Display` for people and `std::error::Error` for other code.

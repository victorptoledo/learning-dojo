---
from: csharp
status: draft
concept: "Build tool and package manager"
from_way: "dotnet CLI with a .csproj, NuGet packages and MSBuild."
native_way: "cargo with Cargo.toml, crates from crates.io, and a native binary as the result."
sensei_tip: "dotnet new, build, test and run map one to one onto cargo new, build, test and run. A Rust release binary is like a .NET self-contained, trimmed AOT publish, but by default."
---

| C# | Rust |
| --- | --- |
| .csproj | Cargo.toml |
| packages.lock.json | Cargo.lock |
| dotnet run | cargo run |
| dotnet test | cargo test |
| dotnet format / analyzers | cargo fmt / cargo clippy |
| NuGet | crates.io |

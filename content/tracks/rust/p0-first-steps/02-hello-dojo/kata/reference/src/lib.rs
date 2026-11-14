/// Greeting shown when someone enters the dojo.
pub fn greet(name: &str) -> String {
    let name = name.trim();
    let who = if name.is_empty() { "stranger" } else { name };
    format!("Welcome to the dojo, {who}!")
}

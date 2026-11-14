use std::collections::HashMap;

/// What happened when stock was taken out.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Removal {
    Done { left: u32 },
    NotEnough { available: u32 },
    Unknown,
}

/// A shop inventory: item name to quantity in stock.
#[derive(Debug, Default)]
pub struct Inventory {
    items: HashMap<String, u32>,
}

impl Inventory {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn add(&mut self, name: &str, quantity: u32) {
        *self.items.entry(name.to_string()).or_insert(0) += quantity;
    }

    pub fn remove(&mut self, name: &str, quantity: u32) -> Removal {
        let Some(available) = self.items.get_mut(name) else {
            return Removal::Unknown;
        };
        if *available < quantity {
            return Removal::NotEnough { available: *available };
        }
        *available -= quantity;
        let left = *available;
        if left == 0 {
            self.items.remove(name);
        }
        Removal::Done { left }
    }

    pub fn quantity(&self, name: &str) -> u32 {
        self.items.get(name).copied().unwrap_or(0)
    }

    pub fn report(&self) -> Vec<String> {
        let mut lines: Vec<String> = self
            .items
            .iter()
            .map(|(name, quantity)| format!("{name}: {quantity}"))
            .collect();
        lines.sort();
        lines
    }

    pub fn low_stock(&self, threshold: u32) -> Vec<&str> {
        let mut names: Vec<&str> = self
            .items
            .iter()
            .filter(|(_, quantity)| **quantity < threshold)
            .map(|(name, _)| name.as_str())
            .collect();
        names.sort_unstable();
        names
    }
}

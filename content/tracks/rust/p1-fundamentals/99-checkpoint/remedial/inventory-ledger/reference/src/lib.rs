use std::collections::HashMap;

/// One change to the stock.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Change {
    Add { item: String, quantity: u32 },
    Remove { item: String, quantity: u32 },
}

/// Stock per item name.
#[derive(Debug, Default)]
pub struct Inventory {
    stock: HashMap<String, u32>,
}

impl Inventory {
    pub fn new() -> Self {
        Self::default()
    }

    /// Applies `change`; a removal that is not possible changes nothing and returns false.
    pub fn apply(&mut self, change: Change) -> bool {
        match change {
            Change::Add { item, quantity } => {
                *self.stock.entry(item).or_insert(0) += quantity;
                true
            }
            Change::Remove { item, quantity } => match self.stock.get_mut(&item) {
                Some(have) if *have >= quantity => {
                    *have -= quantity;
                    true
                }
                _ => false,
            },
        }
    }

    /// How many of `item` are in stock.
    pub fn quantity(&self, item: &str) -> u32 {
        self.stock.get(item).copied().unwrap_or(0)
    }

    /// Items below `threshold`, sorted by name.
    pub fn low_stock(&self, threshold: u32) -> Vec<&str> {
        let mut low: Vec<&str> = self
            .stock
            .iter()
            .filter(|(_, quantity)| **quantity < threshold)
            .map(|(item, _)| item.as_str())
            .collect();
        low.sort_unstable();
        low
    }
}

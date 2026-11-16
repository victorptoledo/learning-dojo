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
        todo!("an empty inventory")
    }

    /// Applies `change`; a removal that is not possible changes nothing and returns false.
    pub fn apply(&mut self, change: Change) -> bool {
        todo!("apply {change:?} to {:?}", self.stock)
    }

    /// How many of `item` are in stock.
    pub fn quantity(&self, item: &str) -> u32 {
        todo!("quantity of {item:?}")
    }

    /// Items below `threshold`, sorted by name.
    pub fn low_stock(&self, threshold: u32) -> Vec<&str> {
        todo!("items below {threshold}")
    }
}

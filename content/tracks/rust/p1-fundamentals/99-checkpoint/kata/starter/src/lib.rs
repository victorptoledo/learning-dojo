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
        todo!("an empty inventory")
    }

    pub fn add(&mut self, name: &str, quantity: u32) {
        todo!("add {quantity} of {name}")
    }

    pub fn remove(&mut self, name: &str, quantity: u32) -> Removal {
        todo!("remove {quantity} of {name}")
    }

    pub fn quantity(&self, name: &str) -> u32 {
        todo!("stock of {name}")
    }

    pub fn report(&self) -> Vec<String> {
        todo!("one line per item, sorted by name")
    }

    pub fn low_stock(&self, threshold: u32) -> Vec<&str> {
        todo!("items below {threshold}")
    }
}

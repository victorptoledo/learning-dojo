#![allow(unused)]
use std::error::Error;
use std::fmt;
use std::future::Future;
use std::num::ParseIntError;
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll, Wake, Waker};
use std::thread::{self, Thread};

/// Pending `remaining` times, waking itself each time, then ready with how often it was polled.
pub struct Countdown {
    remaining: u32,
    polls: u32,
}

pub fn countdown(steps: u32) -> Countdown {
    Countdown { remaining: steps, polls: 0 }
}

impl Future for Countdown {
    type Output = u32;

    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<u32> {
        todo!("count the poll; Pending (and wake) while steps remain, then Ready(polls)")
    }
}

/// Runs a future on this thread, parking while it is pending.
pub fn block_on<F: Future>(future: F) -> F::Output {
    todo!()
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PortError {
    NotANumber { value: String, source: ParseIntError },
    Reserved { port: u16 },
}

impl fmt::Display for PortError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        todo!("the two messages in the README")
    }
}

impl Error for PortError {
    // TODO: source() for NotANumber
}

/// A port from 1024 to 65535, from text with spaces around it.
pub fn parse_port(value: &str) -> Result<u16, PortError> {
    todo!()
}

/// The error and its causes, joined with ": ".
pub fn chain(error: &dyn Error) -> String {
    todo!()
}

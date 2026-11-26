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

    fn poll(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<u32> {
        self.polls += 1;
        if self.remaining == 0 {
            return Poll::Ready(self.polls);
        }
        self.remaining -= 1;
        cx.waker().wake_by_ref(); // ask to be polled again
        Poll::Pending
    }
}

struct ThreadWaker(Thread);

impl Wake for ThreadWaker {
    fn wake(self: Arc<Self>) {
        self.0.unpark();
    }
}

/// Runs a future on this thread, parking while it is pending.
pub fn block_on<F: Future>(future: F) -> F::Output {
    let mut future = std::pin::pin!(future);
    let waker = Waker::from(Arc::new(ThreadWaker(thread::current())));
    let mut cx = Context::from_waker(&waker);
    loop {
        match future.as_mut().poll(&mut cx) {
            Poll::Ready(value) => return value,
            Poll::Pending => thread::park(),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PortError {
    NotANumber { value: String, source: ParseIntError },
    Reserved { port: u16 },
}

impl fmt::Display for PortError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            PortError::NotANumber { value, .. } => write!(f, "`{value}` is not a port"),
            PortError::Reserved { port } => write!(f, "port {port} is reserved (below 1024)"),
        }
    }
}

impl Error for PortError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        match self {
            PortError::NotANumber { source, .. } => Some(source),
            PortError::Reserved { .. } => None,
        }
    }
}

/// A port from 1024 to 65535, from text with spaces around it.
pub fn parse_port(value: &str) -> Result<u16, PortError> {
    let port: u16 = value
        .trim()
        .parse()
        .map_err(|source| PortError::NotANumber { value: value.trim().to_string(), source })?;
    if port < 1024 {
        return Err(PortError::Reserved { port });
    }
    Ok(port)
}

/// The error and its causes, joined with ": ".
pub fn chain(error: &dyn Error) -> String {
    let mut text = error.to_string();
    let mut cause = error.source();
    while let Some(next) = cause {
        text.push_str(": ");
        text.push_str(&next.to_string());
        cause = next.source();
    }
    text
}

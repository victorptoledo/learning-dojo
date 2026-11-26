use kata::{PortError, block_on, chain, countdown, parse_port};
use std::error::Error;
use std::future::Future;
use std::pin::Pin;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::{Arc, mpsc};
use std::task::{Context, Poll, Wake, Waker};
use std::thread;
use std::time::Duration;

struct Counting(AtomicUsize);

impl Wake for Counting {
    fn wake(self: Arc<Self>) {
        self.0.fetch_add(1, Ordering::SeqCst);
    }
}

#[test]
fn countdown_is_pending_and_wakes_itself_until_done() {
    let counter = Arc::new(Counting(AtomicUsize::new(0)));
    let waker = Waker::from(Arc::clone(&counter));
    let mut cx = Context::from_waker(&waker);
    let mut future = Box::pin(countdown(2));
    assert!(future.as_mut().poll(&mut cx).is_pending());
    assert!(future.as_mut().poll(&mut cx).is_pending());
    assert_eq!(future.as_mut().poll(&mut cx), Poll::Ready(3));
    assert_eq!(counter.0.load(Ordering::SeqCst), 2, "each Pending must wake the waker");
}

#[test]
fn a_zero_countdown_is_ready_at_once() {
    assert_eq!(block_on(countdown(0)), 1);
}

#[test]
fn block_on_drives_a_countdown() {
    assert_eq!(block_on(countdown(5)), 6);
}

#[test]
fn block_on_runs_async_blocks_that_await() {
    let total = block_on(async { countdown(1).await + countdown(2).await });
    assert_eq!(total, 5);
}

/// Pending once; woken later from another thread.
struct WokenFromAfar(bool);

impl Future for WokenFromAfar {
    type Output = &'static str;
    fn poll(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<&'static str> {
        if self.0 {
            return Poll::Ready("woken");
        }
        self.0 = true;
        let waker = cx.waker().clone();
        thread::spawn(move || {
            thread::sleep(Duration::from_millis(100));
            waker.wake();
        });
        Poll::Pending
    }
}

#[test]
fn block_on_waits_for_a_wake_from_another_thread() {
    let (sender, receiver) = mpsc::channel();
    thread::spawn(move || {
        let _ = sender.send(block_on(WokenFromAfar(false)));
    });
    assert_eq!(receiver.recv_timeout(Duration::from_secs(5)), Ok("woken"));
}

#[test]
fn parses_ports_with_spaces() {
    assert_eq!(parse_port(" 8080 "), Ok(8080));
    assert_eq!(parse_port("1024"), Ok(1024));
    assert_eq!(parse_port("65535"), Ok(65535));
}

#[test]
fn ports_below_1024_are_reserved() {
    let error = parse_port("80").unwrap_err();
    assert_eq!(error, PortError::Reserved { port: 80 });
    assert_eq!(error.to_string(), "port 80 is reserved (below 1024)");
    assert!(error.source().is_none());
}

#[test]
fn text_that_is_not_a_port_keeps_the_parse_error() {
    let error = parse_port(" http ").unwrap_err();
    assert_eq!(error.to_string(), "`http` is not a port");
    assert!(error.source().unwrap().downcast_ref::<std::num::ParseIntError>().is_some());
    assert_eq!(chain(&error), "`http` is not a port: invalid digit found in string");
}

#[test]
fn a_number_too_large_for_a_port_is_not_a_port() {
    let error = parse_port("70000").unwrap_err();
    assert_eq!(chain(&error), "`70000` is not a port: number too large to fit in target type");
}

#[test]
fn chain_of_an_error_without_a_source_is_its_message() {
    assert_eq!(chain(&parse_port("22").unwrap_err()), "port 22 is reserved (below 1024)");
}

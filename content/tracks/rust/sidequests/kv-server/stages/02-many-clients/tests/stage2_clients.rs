//! Stage 2 contract: many clients at once, and an atomic INCR.

mod support;

use std::thread;
use support::{Client, Server};

#[test]
fn an_idle_client_does_not_block_the_others() {
    let server = Server::start(&[]);
    let _idle = server.client();
    let mut active = server.client();
    assert_eq!(active.send("PING"), "PONG");
}

#[test]
fn clients_see_each_others_writes() {
    let server = Server::start(&[]);
    let mut writer = server.client();
    let mut reader = server.client();
    writer.send("SET news fresh");
    assert_eq!(reader.send("GET news"), "VALUE fresh");
}

#[test]
fn sixteen_clients_work_at_the_same_time() {
    let server = Server::start(&[]);
    let port = server.port;
    let handles: Vec<_> = (0..16)
        .map(|id| {
            thread::spawn(move || {
                let mut client = Client::connect(port);
                for round in 0..50 {
                    let key = format!("c{id}-{round}");
                    assert_eq!(client.send(&format!("SET {key} {id} {round}")), "OK");
                    assert_eq!(client.send(&format!("GET {key}")), format!("VALUE {id} {round}"));
                }
            })
        })
        .collect();
    for handle in handles {
        handle.join().expect("every client got its own replies");
    }
    assert_eq!(server.client().send("GET c15-49"), "VALUE 15 49");
}

#[test]
fn incr_starts_from_zero_and_counts_up() {
    let server = Server::start(&[]);
    let mut client = server.client();
    assert_eq!(client.send("INCR visits"), "VALUE 1");
    assert_eq!(client.send("INCR visits"), "VALUE 2");
    client.send("SET score 40");
    assert_eq!(client.send("INCR score"), "VALUE 41");
    assert_eq!(client.send("GET score"), "VALUE 41");
}

#[test]
fn incr_on_text_is_an_error_and_changes_nothing() {
    let server = Server::start(&[]);
    let mut client = server.client();
    client.send("SET name ada");
    assert_eq!(client.send("INCR name"), "ERROR not an integer");
    assert_eq!(client.send("GET name"), "VALUE ada");
    assert_eq!(client.send("INCR"), "ERROR wrong number of arguments");
}

#[test]
fn concurrent_incr_loses_no_update() {
    let server = Server::start(&[]);
    let port = server.port;
    let handles: Vec<_> = (0..8)
        .map(|_| {
            thread::spawn(move || {
                let mut client = Client::connect(port);
                for _ in 0..100 {
                    assert!(client.send("INCR hits").starts_with("VALUE "));
                }
            })
        })
        .collect();
    for handle in handles {
        handle.join().unwrap();
    }
    assert_eq!(server.client().send("GET hits"), "VALUE 800");
}

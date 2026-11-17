//! Stage 4 contract: EXPIRE and TTL, long lines and --max-clients.

mod support;

use std::thread;
use std::time::Duration;
use support::{Client, Server};

#[test]
fn a_key_disappears_after_its_expiry() {
    let server = Server::start(&[]);
    let mut client = server.client();
    client.send("SET session abc");
    assert_eq!(client.send("EXPIRE session 1"), "OK");
    assert_eq!(client.send("GET session"), "VALUE abc");
    thread::sleep(Duration::from_millis(1_300));
    assert_eq!(client.send("GET session"), "NOT_FOUND");
    assert_eq!(client.send("TTL session"), "NOT_FOUND");
    assert_eq!(client.send("DEL session"), "NOT_FOUND");
}

#[test]
fn ttl_counts_seconds_left_rounded_up() {
    let server = Server::start(&[]);
    let mut client = server.client();
    client.send("SET k v");
    assert_eq!(client.send("TTL k"), "VALUE -1");
    client.send("EXPIRE k 100");
    assert_eq!(client.send("TTL k"), "VALUE 100");
    assert_eq!(client.send("TTL missing"), "NOT_FOUND");
}

#[test]
fn set_removes_an_expiry() {
    let server = Server::start(&[]);
    let mut client = server.client();
    client.send("SET k v");
    client.send("EXPIRE k 1");
    client.send("SET k fresh");
    assert_eq!(client.send("TTL k"), "VALUE -1");
    thread::sleep(Duration::from_millis(1_300));
    assert_eq!(client.send("GET k"), "VALUE fresh");
}

#[test]
fn expire_checks_its_arguments() {
    let server = Server::start(&[]);
    let mut client = server.client();
    client.send("SET k v");
    assert_eq!(client.send("EXPIRE missing 10"), "NOT_FOUND");
    assert_eq!(client.send("EXPIRE k soon"), "ERROR not an integer");
    assert_eq!(client.send("EXPIRE k -5"), "ERROR not an integer");
    assert_eq!(client.send("EXPIRE k"), "ERROR wrong number of arguments");
}

#[test]
fn a_line_too_long_closes_the_connection() {
    let server = Server::start(&[]);
    let mut client = server.client();
    let value = "x".repeat(5_000);
    client.write(format!("SET big {value}\n").as_bytes());
    assert_eq!(client.read_line().as_deref(), Some("ERROR line too long"));
    assert_eq!(client.read_line(), None, "the server closed the connection");
    let mut other = server.client();
    assert_eq!(other.send("GET big"), "NOT_FOUND");
}

#[test]
fn a_line_of_exactly_4096_bytes_is_fine() {
    let server = Server::start(&[]);
    let mut client = server.client();
    let value = "y".repeat(4096 - "SET k ".len());
    assert_eq!(client.send(&format!("SET k {value}")), "OK");
}

#[test]
fn clients_beyond_the_limit_are_turned_away() {
    let server = Server::start(&["--max-clients", "2"]);
    let mut first = server.client();
    let mut second = server.client();
    assert_eq!(first.send("PING"), "PONG");
    assert_eq!(second.send("PING"), "PONG");
    let mut third = server.client();
    assert_eq!(third.read_line().as_deref(), Some("ERROR too many clients"));
    assert_eq!(third.read_line(), None);

    drop(second);
    // The server notices the closed connection; give it a moment.
    let mut retry = None;
    for _ in 0..50 {
        let mut client = Client::connect(server.port);
        client.write(b"PING\n");
        if client.read_line().as_deref() == Some("PONG") {
            retry = Some(client);
            break;
        }
        thread::sleep(Duration::from_millis(50));
    }
    assert!(retry.is_some(), "a freed place can be taken again");
    assert_eq!(first.send("PING"), "PONG");
}

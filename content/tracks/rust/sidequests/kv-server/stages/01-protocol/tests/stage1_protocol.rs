//! Stage 1 contract: PING, SET, GET, DEL and errors over one connection at a time.

mod support;

use support::Server;

#[test]
fn ping_replies_pong() {
    let server = Server::start(&[]);
    assert_eq!(server.client().send("PING"), "PONG");
}

#[test]
fn a_value_set_can_be_read_back() {
    let server = Server::start(&[]);
    let mut client = server.client();
    assert_eq!(client.send("SET name ada"), "OK");
    assert_eq!(client.send("GET name"), "VALUE ada");
}

#[test]
fn a_missing_key_is_not_found() {
    let server = Server::start(&[]);
    assert_eq!(server.client().send("GET nothing"), "NOT_FOUND");
}

#[test]
fn values_keep_their_spaces() {
    let server = Server::start(&[]);
    let mut client = server.client();
    assert_eq!(client.send("SET greeting hello there,  friend"), "OK");
    assert_eq!(client.send("GET greeting"), "VALUE hello there,  friend");
}

#[test]
fn set_replaces_the_value() {
    let server = Server::start(&[]);
    let mut client = server.client();
    client.send("SET k one");
    client.send("SET k two");
    assert_eq!(client.send("GET k"), "VALUE two");
}

#[test]
fn del_removes_a_key_once() {
    let server = Server::start(&[]);
    let mut client = server.client();
    client.send("SET k v");
    assert_eq!(client.send("DEL k"), "DELETED");
    assert_eq!(client.send("GET k"), "NOT_FOUND");
    assert_eq!(client.send("DEL k"), "NOT_FOUND");
}

#[test]
fn unknown_commands_and_missing_arguments_are_errors() {
    let server = Server::start(&[]);
    let mut client = server.client();
    assert_eq!(client.send("FLY away"), "ERROR unknown command");
    assert_eq!(client.send("GET"), "ERROR wrong number of arguments");
    assert_eq!(client.send("SET lonely"), "ERROR wrong number of arguments");
    assert_eq!(client.send("DEL"), "ERROR wrong number of arguments");
    assert_eq!(
        client.send("PING"),
        "PONG",
        "the connection survives errors"
    );
}

#[test]
fn windows_line_endings_are_accepted() {
    let server = Server::start(&[]);
    let mut client = server.client();
    client.write(b"SET crlf yes\r\n");
    assert_eq!(client.read_line().as_deref(), Some("OK"));
    assert_eq!(client.send("GET crlf"), "VALUE yes");
}

#[test]
fn data_stays_for_the_next_client() {
    let server = Server::start(&[]);
    {
        let mut first = server.client();
        assert_eq!(first.send("SET shared 42"), "OK");
    }
    let mut second = server.client();
    assert_eq!(second.send("GET shared"), "VALUE 42");
}

#[test]
fn several_commands_sent_at_once_get_replies_in_order() {
    let server = Server::start(&[]);
    let mut client = server.client();
    client.write(b"SET a 1\nSET b 2\nGET a\nGET b\n");
    let replies: Vec<String> = (0..4).filter_map(|_| client.read_line()).collect();
    assert_eq!(replies, ["OK", "OK", "VALUE 1", "VALUE 2"]);
}

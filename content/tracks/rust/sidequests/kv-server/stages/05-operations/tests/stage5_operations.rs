//! Stage 5 contract: STATS, SHUTDOWN, KVD_PORT and Unix-style command-line errors.

mod support;

use std::net::TcpListener;
use std::process::{Command, Stdio};
use std::time::Duration;
use support::{Server, temp_dir};

fn kvd(args: &[&str]) -> (i32, String) {
    let output = Command::new(env!("CARGO_BIN_EXE_kvd"))
        .args(args)
        .env_remove("KVD_PORT")
        .stdin(Stdio::null())
        .output()
        .expect("kvd runs");
    (
        output.status.code().unwrap_or(-1),
        String::from_utf8_lossy(&output.stderr).into_owned(),
    )
}

#[test]
fn stats_counts_keys_clients_and_commands() {
    let server = Server::start(&[]);
    let mut client = server.client();
    let _other = server.client();
    client.send("SET a 1");
    client.send("SET b 2");
    client.send("GET a");
    assert_eq!(client.send("STATS"), "VALUE keys=2 clients=2 commands=4");
}

#[test]
fn stats_leaves_out_expired_keys() {
    let server = Server::start(&[]);
    let mut client = server.client();
    client.send("SET a 1");
    client.send("SET b 2");
    client.send("EXPIRE b 1");
    std::thread::sleep(Duration::from_millis(1_300));
    assert!(client.send("STATS").starts_with("VALUE keys=1 clients=1 "));
}

#[test]
fn shutdown_replies_then_exits_with_code_0() {
    let mut server = Server::start(&[]);
    let mut client = server.client();
    assert_eq!(client.send("SHUTDOWN"), "OK");
    let status = server.wait_exit(Duration::from_secs(2));
    assert_eq!(status.and_then(|status| status.code()), Some(0));
}

#[test]
fn shutdown_works_while_other_clients_stay_connected() {
    let mut server = Server::start(&[]);
    let _idle = server.client();
    assert_eq!(server.client().send("SHUTDOWN"), "OK");
    let status = server.wait_exit(Duration::from_secs(2));
    assert_eq!(status.and_then(|status| status.code()), Some(0));
}

#[test]
fn shutdown_keeps_the_data() {
    let dir = temp_dir("shutdown");
    let data = dir.join("kvd.data");
    let data_arg = data.to_str().unwrap();
    {
        let mut server = Server::start(&["--data", data_arg]);
        let mut client = server.client();
        client.send("SET kept yes");
        client.send("SHUTDOWN");
        server.wait_exit(Duration::from_secs(2));
    }
    let server = Server::start(&["--data", data_arg]);
    assert_eq!(server.client().send("GET kept"), "VALUE yes");
}

#[test]
fn the_port_can_come_from_kvd_port() {
    let free = TcpListener::bind("127.0.0.1:0").unwrap();
    let port = free.local_addr().unwrap().port();
    drop(free);
    let mut command = Command::new(env!("CARGO_BIN_EXE_kvd"));
    command.env("KVD_PORT", port.to_string());
    let server = Server::spawn(command);
    assert_eq!(server.port, port);
    assert_eq!(server.client().send("PING"), "PONG");
}

#[test]
fn a_bad_command_line_prints_usage_and_exits_with_2() {
    for args in [
        vec![],
        vec!["--port"],
        vec!["--port", "eighty"],
        vec!["--port", "0", "--colour"],
        vec!["--port", "0", "--data"],
    ] {
        let (code, stderr) = kvd(&args);
        assert_eq!(code, 2, "kvd {args:?} should exit 2");
        assert!(stderr.starts_with("usage: kvd"), "kvd {args:?}: stderr was {stderr:?}");
    }
}

#[test]
fn a_port_in_use_is_an_error_with_exit_code_1() {
    let taken = TcpListener::bind("127.0.0.1:0").unwrap();
    let port = taken.local_addr().unwrap().port().to_string();
    let (code, stderr) = kvd(&["--port", &port]);
    assert_eq!(code, 1);
    assert!(stderr.starts_with("error: "), "{stderr:?}");
}

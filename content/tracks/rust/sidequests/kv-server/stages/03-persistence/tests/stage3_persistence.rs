//! Stage 3 contract: --data keeps every acknowledged write, safely, through a SIGKILL.

mod support;

use std::fs;
use std::os::unix::fs::PermissionsExt;
use support::{Server, temp_dir};

fn start(data: &std::path::Path) -> Server {
    Server::start(&["--data", data.to_str().unwrap()])
}

#[test]
fn writes_survive_a_kill_and_a_restart() {
    let dir = temp_dir("restart");
    let data = dir.join("kvd.data");
    {
        let mut server = start(&data);
        let mut client = server.client();
        assert_eq!(client.send("SET name ada lovelace"), "OK");
        assert_eq!(client.send("SET gone soon"), "OK");
        assert_eq!(client.send("INCR count"), "VALUE 1");
        assert_eq!(client.send("INCR count"), "VALUE 2");
        assert_eq!(client.send("DEL gone"), "DELETED");
        server.kill();
    }
    let server = start(&data);
    let mut client = server.client();
    assert_eq!(client.send("GET name"), "VALUE ada lovelace");
    assert_eq!(client.send("GET count"), "VALUE 2");
    assert_eq!(client.send("GET gone"), "NOT_FOUND");
}

#[test]
fn a_missing_data_file_means_no_data_yet() {
    let dir = temp_dir("fresh");
    let server = start(&dir.join("new.data"));
    assert_eq!(server.client().send("GET anything"), "NOT_FOUND");
}

#[test]
fn the_data_file_is_private_to_its_owner() {
    let dir = temp_dir("mode");
    let data = dir.join("kvd.data");
    let server = start(&data);
    server.client().send("SET secret 1");
    let mode = fs::metadata(&data).expect("the data file exists").permissions().mode();
    assert_eq!(mode & 0o777, 0o600, "mode was {:o}", mode & 0o777);
}

#[test]
fn no_temporary_file_is_left_behind() {
    let dir = temp_dir("tidy");
    let data = dir.join("kvd.data");
    let mut server = start(&data);
    let mut client = server.client();
    for i in 0..20 {
        client.send(&format!("SET k{i} {i}"));
    }
    server.kill();
    let names: Vec<String> = fs::read_dir(&dir)
        .unwrap()
        .map(|entry| entry.unwrap().file_name().to_string_lossy().into_owned())
        .collect();
    assert_eq!(names, ["kvd.data"], "only the data file stays in its folder");
}

#[test]
fn many_restarts_keep_everything() {
    let dir = temp_dir("many");
    let data = dir.join("kvd.data");
    for round in 0..3 {
        let mut server = start(&data);
        let mut client = server.client();
        assert_eq!(client.send(&format!("SET round{round} value {round}")), "OK");
        for earlier in 0..round {
            assert_eq!(
                client.send(&format!("GET round{earlier}")),
                format!("VALUE value {earlier}")
            );
        }
        server.kill();
    }
}

#[test]
fn without_data_nothing_is_kept() {
    {
        let mut server = Server::start(&[]);
        server.client().send("SET k v");
        server.kill();
    }
    let server = Server::start(&[]);
    assert_eq!(server.client().send("GET k"), "NOT_FOUND");
}

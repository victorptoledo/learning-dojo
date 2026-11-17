//! Shared by every stage: starts `kvd` as a process and talks to it over TCP.
#![allow(dead_code)]

use std::io::{BufRead, BufReader, Read, Write};
use std::net::TcpStream;
use std::path::PathBuf;
use std::process::{Child, Command, ExitStatus, Stdio};
use std::sync::mpsc;
use std::thread;
use std::time::{Duration, Instant};

pub const READ_TIMEOUT: Duration = Duration::from_secs(5);

pub struct Server {
    child: Child,
    pub port: u16,
}

impl Server {
    /// `kvd --port 0 <args>`, waiting for its `listening on 127.0.0.1:<port>` line.
    pub fn start(args: &[&str]) -> Server {
        let mut command = Command::new(env!("CARGO_BIN_EXE_kvd"));
        command.args(["--port", "0"]).args(args);
        Server::spawn(command)
    }

    pub fn spawn(mut command: Command) -> Server {
        let mut child = command
            .stdin(Stdio::null())
            .stdout(Stdio::piped())
            .stderr(Stdio::null())
            .spawn()
            .expect("the kvd binary starts");
        let stdout = child.stdout.take().unwrap();
        let (sender, receiver) = mpsc::channel();
        thread::spawn(move || {
            let mut reader = BufReader::new(stdout);
            let mut first = String::new();
            let _ = reader.read_line(&mut first);
            let _ = sender.send(first);
            // Keep draining so the server never blocks on a full pipe.
            let mut rest = Vec::new();
            let _ = reader.read_to_end(&mut rest);
        });
        let first = match receiver.recv_timeout(Duration::from_secs(10)) {
            Ok(line) => line,
            Err(_) => {
                let _ = child.kill();
                panic!("kvd printed no `listening on 127.0.0.1:<port>` line within 10 s");
            }
        };
        let port = first
            .trim()
            .strip_prefix("listening on 127.0.0.1:")
            .and_then(|port| port.parse().ok())
            .unwrap_or_else(|| {
                let _ = child.kill();
                panic!(
                    "the first stdout line should be `listening on 127.0.0.1:<port>`, was {first:?}"
                )
            });
        Server { child, port }
    }

    pub fn client(&self) -> Client {
        Client::connect(self.port)
    }

    /// SIGKILL: the process gets no chance to clean up.
    pub fn kill(&mut self) {
        let _ = self.child.kill();
        let _ = self.child.wait();
    }

    /// The exit status if the process ends within `timeout`.
    pub fn wait_exit(&mut self, timeout: Duration) -> Option<ExitStatus> {
        let deadline = Instant::now() + timeout;
        while Instant::now() < deadline {
            if let Ok(Some(status)) = self.child.try_wait() {
                return Some(status);
            }
            thread::sleep(Duration::from_millis(20));
        }
        None
    }
}

impl Drop for Server {
    fn drop(&mut self) {
        self.kill();
    }
}

pub struct Client {
    reader: BufReader<TcpStream>,
    writer: TcpStream,
}

impl Client {
    pub fn connect(port: u16) -> Client {
        let stream = TcpStream::connect(("127.0.0.1", port)).expect("kvd accepts connections");
        stream.set_read_timeout(Some(READ_TIMEOUT)).unwrap();
        Client {
            reader: BufReader::new(stream.try_clone().unwrap()),
            writer: stream,
        }
    }

    /// Sends one command line and returns the reply line, without its line ending.
    pub fn send(&mut self, command: &str) -> String {
        self.write(format!("{command}\n").as_bytes());
        self.read_line()
            .unwrap_or_else(|| panic!("no reply to {command:?}: the connection closed"))
    }

    pub fn write(&mut self, bytes: &[u8]) {
        self.writer
            .write_all(bytes)
            .expect("the connection is open");
        self.writer.flush().unwrap();
    }

    /// The next line, or `None` when the server closed the connection.
    pub fn read_line(&mut self) -> Option<String> {
        let mut line = String::new();
        match self.reader.read_line(&mut line) {
            Ok(0) => None,
            Ok(_) => Some(line.trim_end_matches(['\r', '\n']).to_string()),
            Err(error) => panic!("no reply within {READ_TIMEOUT:?}: {error}"),
        }
    }
}

/// A fresh, empty folder for one test.
pub fn temp_dir(name: &str) -> PathBuf {
    let dir = std::env::temp_dir().join(format!("kvd-{}-{name}", std::process::id()));
    let _ = std::fs::remove_dir_all(&dir);
    std::fs::create_dir_all(&dir).unwrap();
    dir
}

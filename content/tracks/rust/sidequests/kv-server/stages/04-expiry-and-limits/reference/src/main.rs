//! kvd (stage 4): a key-value server over TCP, one thread per client, one shared store.

use std::collections::HashMap;
use std::io::{self, BufRead, BufReader, Read, Write};
use std::net::{TcpListener, TcpStream};
use std::path::{Path, PathBuf};
use std::process::ExitCode;
use std::sync::atomic::{AtomicU64, AtomicUsize, Ordering};
use std::sync::{Arc, Mutex, MutexGuard};
use std::thread;
use std::time::{Duration, Instant};

const MAX_LINE: usize = 4096;
const USAGE: &str = "usage: kvd [--port <port>] [--data <file>] [--max-clients <n>] (or KVD_PORT)";

struct Config {
    port: u16,
    data: Option<PathBuf>,
    max_clients: usize,
}

fn parse_args(args: &[String], env_port: Option<String>) -> Result<Config, ()> {
    let mut port = env_port;
    let mut data = None;
    let mut max_clients = 64;
    let mut args = args.iter();
    while let Some(flag) = args.next() {
        let value = args.next().ok_or(())?;
        match flag.as_str() {
            "--port" => port = Some(value.clone()),
            "--data" => data = Some(PathBuf::from(value)),
            "--max-clients" => max_clients = value.parse().map_err(|_| ())?,
            _ => return Err(()),
        }
    }
    let port = port.ok_or(())?.parse().map_err(|_| ())?;
    Ok(Config {
        port,
        data,
        max_clients,
    })
}

struct Entry {
    value: String,
    expires: Option<Instant>,
}

impl Entry {
    fn is_live(&self, now: Instant) -> bool {
        self.expires.is_none_or(|at| at > now)
    }
}

struct Store {
    entries: HashMap<String, Entry>,
    file: Option<PathBuf>,
}

impl Store {
    fn open(file: Option<PathBuf>) -> io::Result<Self> {
        let mut entries = HashMap::new();
        if let Some(path) = &file {
            match std::fs::read_to_string(path) {
                Ok(text) => {
                    for line in text.lines() {
                        if let Some((key, value)) = line.split_once(' ') {
                            let value = value.to_string();
                            entries.insert(
                                key.to_string(),
                                Entry {
                                    value,
                                    expires: None,
                                },
                            );
                        }
                    }
                }
                Err(error) if error.kind() == io::ErrorKind::NotFound => {}
                Err(error) => return Err(error),
            }
        }
        Ok(Self { entries, file })
    }

    /// The live entry of `key`; an expired one is removed first.
    fn live(&mut self, key: &str) -> Option<&mut Entry> {
        let now = Instant::now();
        if self
            .entries
            .get(key)
            .is_some_and(|entry| !entry.is_live(now))
        {
            self.entries.remove(key);
        }
        self.entries.get_mut(key)
    }

    fn live_count(&self) -> usize {
        let now = Instant::now();
        self.entries
            .values()
            .filter(|entry| entry.is_live(now))
            .count()
    }

    /// Writes every live entry to a temporary file next to the data file, then renames
    /// it over the data file: a crash leaves the old file or the new one, never half.
    fn save(&self) -> io::Result<()> {
        let Some(path) = &self.file else {
            return Ok(());
        };
        let now = Instant::now();
        let mut text = String::new();
        for (key, entry) in self.entries.iter().filter(|(_, entry)| entry.is_live(now)) {
            text.push_str(key);
            text.push(' ');
            text.push_str(&entry.value);
            text.push('\n');
        }
        let temporary = temporary_path(path);
        let mut file = private_file(&temporary)?;
        file.write_all(text.as_bytes())?;
        file.sync_all()?;
        std::fs::rename(&temporary, path)
    }
}

fn temporary_path(path: &Path) -> PathBuf {
    let mut name = path.file_name().unwrap_or_default().to_os_string();
    name.push(".tmp");
    path.with_file_name(name)
}

#[cfg(unix)]
fn private_file(path: &Path) -> io::Result<std::fs::File> {
    use std::os::unix::fs::OpenOptionsExt;
    std::fs::OpenOptions::new()
        .write(true)
        .create(true)
        .truncate(true)
        .mode(0o600)
        .open(path)
}

#[cfg(not(unix))]
fn private_file(path: &Path) -> io::Result<std::fs::File> {
    std::fs::File::create(path)
}

struct Shared {
    store: Mutex<Store>,
    clients: AtomicUsize,
    commands: AtomicU64,
    max_clients: usize,
}

impl Shared {
    fn store(&self) -> MutexGuard<'_, Store> {
        self.store
            .lock()
            .unwrap_or_else(|poisoned| poisoned.into_inner())
    }
}

enum Reply {
    Line(String),
    Shutdown,
}

fn line(text: impl Into<String>) -> Reply {
    Reply::Line(text.into())
}

fn handle(shared: &Shared, command: &str) -> Reply {
    shared.commands.fetch_add(1, Ordering::SeqCst);
    let (name, rest) = command.split_once(' ').unwrap_or((command, ""));
    let mut words = rest.split(' ').filter(|word| !word.is_empty());
    let wrong = || line("ERROR wrong number of arguments");
    match name {
        "PING" => line("PONG"),
        "GET" => {
            let Some(key) = words.next() else {
                return wrong();
            };
            match shared.store().live(key) {
                Some(entry) => line(format!("VALUE {}", entry.value)),
                None => line("NOT_FOUND"),
            }
        }
        "SET" => {
            let Some((key, value)) = rest
                .split_once(' ')
                .filter(|(k, v)| !k.is_empty() && !v.is_empty())
            else {
                return wrong();
            };
            let mut store = shared.store();
            let entry = Entry {
                value: value.to_string(),
                expires: None,
            };
            store.entries.insert(key.to_string(), entry);
            saved(&store, "OK")
        }
        "DEL" => {
            let Some(key) = words.next() else {
                return wrong();
            };
            let mut store = shared.store();
            if store.live(key).is_none() {
                return line("NOT_FOUND");
            }
            store.entries.remove(key);
            saved(&store, "DELETED")
        }
        "INCR" => {
            let Some(key) = words.next() else {
                return wrong();
            };
            let mut store = shared.store();
            let current = store
                .live(key)
                .map_or(Ok(0), |entry| entry.value.parse::<i64>());
            let Ok(current) = current else {
                return line("ERROR not an integer");
            };
            let next = current + 1;
            let expires = store.live(key).and_then(|entry| entry.expires);
            let entry = Entry {
                value: next.to_string(),
                expires,
            };
            store.entries.insert(key.to_string(), entry);
            saved(&store, &format!("VALUE {next}"))
        }
        "EXPIRE" => {
            let (Some(key), Some(seconds)) = (words.next(), words.next()) else {
                return wrong();
            };
            let Ok(seconds) = seconds
                .parse::<u64>()
                .map_err(|_| ())
                .and_then(|s| if s > 0 { Ok(s) } else { Err(()) })
            else {
                return line("ERROR not an integer");
            };
            match shared.store().live(key) {
                Some(entry) => {
                    entry.expires = Some(Instant::now() + Duration::from_secs(seconds));
                    line("OK")
                }
                None => line("NOT_FOUND"),
            }
        }
        "TTL" => {
            let Some(key) = words.next() else {
                return wrong();
            };
            match shared.store().live(key) {
                None => line("NOT_FOUND"),
                Some(Entry { expires: None, .. }) => line("VALUE -1"),
                Some(Entry {
                    expires: Some(at), ..
                }) => {
                    let left = at.saturating_duration_since(Instant::now()).as_millis();
                    line(format!("VALUE {}", left.div_ceil(1000)))
                }
            }
        }
        _ => line("ERROR unknown command"),
    }
}

/// The reply once the store is on disk; a failed write is reported to the client.
fn saved(store: &Store, ok: &str) -> Reply {
    match store.save() {
        Ok(()) => line(ok),
        Err(error) => line(format!("ERROR cannot save: {error}")),
    }
}

/// One line of at most `MAX_LINE` bytes: `Ok(None)` at the end, `Err(())` when too long.
fn read_command(reader: &mut BufReader<TcpStream>) -> io::Result<Option<Result<String, ()>>> {
    let mut bytes = Vec::new();
    let read = reader
        .by_ref()
        .take(MAX_LINE as u64 + 2)
        .read_until(b'\n', &mut bytes)?;
    if read == 0 {
        return Ok(None);
    }
    while matches!(bytes.last(), Some(b'\n' | b'\r')) {
        bytes.pop();
    }
    if bytes.len() > MAX_LINE {
        return Ok(Some(Err(())));
    }
    Ok(Some(Ok(String::from_utf8_lossy(&bytes).into_owned())))
}

/// Closing a socket that still holds unread input sends a reset, which can make the client
/// lose the reply. So: stop writing, then read and drop what the client already sent.
fn close_after_reply(writer: &TcpStream, reader: BufReader<TcpStream>) -> io::Result<()> {
    writer.shutdown(std::net::Shutdown::Write)?;
    let stream = reader.into_inner();
    stream.set_read_timeout(Some(Duration::from_millis(500)))?;
    let _ = io::copy(&mut (&stream).take(1 << 20), &mut io::sink());
    Ok(())
}

fn serve(shared: &Shared, stream: TcpStream) -> io::Result<()> {
    let mut writer = stream.try_clone()?;
    let mut reader = BufReader::new(stream);
    while let Some(command) = read_command(&mut reader)? {
        let Ok(command) = command else {
            writer.write_all(b"ERROR line too long\n")?;
            return close_after_reply(&writer, reader);
        };
        if command.is_empty() {
            continue;
        }
        match handle(shared, &command) {
            Reply::Line(reply) => writer.write_all(format!("{reply}\n").as_bytes())?,
            Reply::Shutdown => {
                // Every write is already on disk; save once more for good measure.
                let _ = shared.store().save();
                writer.write_all(b"OK\n")?;
                writer.flush()?;
                std::process::exit(0);
            }
        }
    }
    Ok(())
}

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    let Ok(config) = parse_args(&args, std::env::var("KVD_PORT").ok()) else {
        eprintln!("{USAGE}");
        return ExitCode::from(2);
    };
    let store = match Store::open(config.data) {
        Ok(store) => store,
        Err(error) => {
            eprintln!("error: cannot read the data file: {error}");
            return ExitCode::FAILURE;
        }
    };
    let listener = match TcpListener::bind(("127.0.0.1", config.port)) {
        Ok(listener) => listener,
        Err(error) => {
            eprintln!("error: cannot listen on port {}: {error}", config.port);
            return ExitCode::FAILURE;
        }
    };
    let port = listener
        .local_addr()
        .map(|addr| addr.port())
        .unwrap_or(config.port);
    println!("listening on 127.0.0.1:{port}");
    let _ = io::stdout().flush();
    let shared = Arc::new(Shared {
        store: Mutex::new(store),
        clients: AtomicUsize::new(0),
        commands: AtomicU64::new(0),
        max_clients: config.max_clients,
    });
    for stream in listener.incoming() {
        let Ok(mut stream) = stream else { continue };
        if shared.clients.fetch_add(1, Ordering::SeqCst) >= shared.max_clients {
            shared.clients.fetch_sub(1, Ordering::SeqCst);
            let _ = stream.write_all(b"ERROR too many clients\n");
            continue;
        }
        let shared = Arc::clone(&shared);
        thread::spawn(move || {
            let _ = serve(&shared, stream);
            shared.clients.fetch_sub(1, Ordering::SeqCst);
        });
    }
    ExitCode::SUCCESS
}

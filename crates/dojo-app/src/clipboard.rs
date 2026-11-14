//! Copies text to the system clipboard with the platform's own command, so the dojo needs no
//! extra library: `clip` on Windows, `pbcopy` on macOS, `wl-copy` or `xclip` on Linux.

use std::io::Write;
use std::process::{Command, Stdio};

pub fn copy(text: &str) -> Result<(), String> {
    if cfg!(windows) {
        // clip.exe reads UTF-16LE when the input starts with a byte order mark.
        let mut bytes = vec![0xFF, 0xFE];
        bytes.extend(text.encode_utf16().flat_map(u16::to_le_bytes));
        return pipe("clip", &[], &bytes);
    }
    if cfg!(target_os = "macos") {
        return pipe("pbcopy", &[], text.as_bytes());
    }
    pipe("wl-copy", &[], text.as_bytes())
        .or_else(|_| pipe("xclip", &["-selection", "clipboard"], text.as_bytes()))
}

fn pipe(command: &str, args: &[&str], input: &[u8]) -> Result<(), String> {
    let mut child = Command::new(command)
        .args(args)
        .stdin(Stdio::piped())
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()
        .map_err(|error| format!("cannot run {command}: {error}"))?;
    child
        .stdin
        .take()
        .ok_or("no stdin")?
        .write_all(input)
        .map_err(|error| format!("cannot write to {command}: {error}"))?;
    let status = child.wait().map_err(|error| error.to_string())?;
    if status.success() {
        Ok(())
    } else {
        Err(format!("{command} failed"))
    }
}

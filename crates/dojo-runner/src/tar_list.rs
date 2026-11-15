//! Lists the paths in a tar stream (`docker export`) without unpacking it.
//!
//! Handles ustar names with a prefix, GNU long names (`L`) and PAX `path` records,
//! which is what Go's `archive/tar` (used by Docker) writes.

use std::io::{self, Read};

const BLOCK: usize = 512;

/// Every entry as an absolute path; directories end with `/`.
pub fn tar_paths(mut reader: impl Read) -> io::Result<Vec<String>> {
    let mut paths = Vec::new();
    let mut header = [0u8; BLOCK];
    let mut long_name: Option<String> = None;
    loop {
        if !read_block(&mut reader, &mut header)? || header.iter().all(|&byte| byte == 0) {
            return Ok(paths);
        }
        let size = octal(&header[124..136])?;
        let kind = header[156];
        match kind {
            b'L' => long_name = Some(trim_nul(&read_data(&mut reader, size)?)),
            b'x' => {
                if let Some(path) = pax_path(&read_data(&mut reader, size)?) {
                    long_name = Some(path);
                }
            }
            b'g' => skip_data(&mut reader, size)?,
            _ => {
                let name = long_name.take().unwrap_or_else(|| ustar_name(&header));
                let name = name.trim_start_matches("./").trim_start_matches('/');
                if !name.is_empty() {
                    let mut path = format!("/{name}");
                    if kind == b'5' && !path.ends_with('/') {
                        path.push('/');
                    }
                    paths.push(path);
                }
                skip_data(&mut reader, size)?;
            }
        }
    }
}

/// False at a clean end of stream.
fn read_block(reader: &mut impl Read, block: &mut [u8; BLOCK]) -> io::Result<bool> {
    let mut filled = 0;
    while filled < BLOCK {
        let read = reader.read(&mut block[filled..])?;
        if read == 0 {
            return if filled == 0 {
                Ok(false)
            } else {
                Err(io::Error::new(
                    io::ErrorKind::UnexpectedEof,
                    "truncated tar header",
                ))
            };
        }
        filled += read;
    }
    Ok(true)
}

fn padded(size: u64) -> u64 {
    size.div_ceil(BLOCK as u64) * BLOCK as u64
}

fn read_data(reader: &mut impl Read, size: u64) -> io::Result<Vec<u8>> {
    let mut data = vec![0u8; padded(size) as usize];
    reader.read_exact(&mut data)?;
    data.truncate(size as usize);
    Ok(data)
}

fn skip_data(reader: &mut impl Read, size: u64) -> io::Result<()> {
    let wanted = padded(size);
    let skipped = io::copy(&mut reader.take(wanted), &mut io::sink())?;
    if skipped == wanted {
        Ok(())
    } else {
        Err(io::Error::new(
            io::ErrorKind::UnexpectedEof,
            "truncated tar entry",
        ))
    }
}

fn octal(field: &[u8]) -> io::Result<u64> {
    let text = trim_nul(field);
    let text = text.trim();
    if text.is_empty() {
        return Ok(0);
    }
    u64::from_str_radix(text, 8)
        .map_err(|_| io::Error::new(io::ErrorKind::InvalidData, "bad tar size field"))
}

fn trim_nul(bytes: &[u8]) -> String {
    let end = bytes
        .iter()
        .position(|&byte| byte == 0)
        .unwrap_or(bytes.len());
    String::from_utf8_lossy(&bytes[..end]).into_owned()
}

fn ustar_name(header: &[u8; BLOCK]) -> String {
    let name = trim_nul(&header[0..100]);
    let prefix = if &header[257..262] == b"ustar" {
        trim_nul(&header[345..500])
    } else {
        String::new()
    };
    if prefix.is_empty() {
        name
    } else {
        format!("{prefix}/{name}")
    }
}

/// PAX records are `"<len> <key>=<value>\n"`.
fn pax_path(data: &[u8]) -> Option<String> {
    let text = String::from_utf8_lossy(data);
    text.lines().find_map(|record| {
        let (_, pair) = record.split_once(' ')?;
        pair.strip_prefix("path=").map(str::to_string)
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    fn header(name: &str, kind: u8, size: usize) -> Vec<u8> {
        let mut block = vec![0u8; BLOCK];
        block[..name.len()].copy_from_slice(name.as_bytes());
        let size_field = format!("{size:011o}\0");
        block[124..136].copy_from_slice(size_field.as_bytes());
        block[156] = kind;
        block[257..263].copy_from_slice(b"ustar\0");
        block
    }

    fn entry(name: &str, kind: u8, data: &[u8]) -> Vec<u8> {
        let mut bytes = header(name, kind, data.len());
        bytes.extend_from_slice(data);
        bytes.resize(
            bytes.len() + (padded(data.len() as u64) as usize - data.len()),
            0,
        );
        bytes
    }

    fn end() -> Vec<u8> {
        vec![0u8; BLOCK * 2]
    }

    #[test]
    fn lists_files_and_directories_as_absolute_paths() {
        let mut tar = entry("etc/", b'5', b"");
        tar.extend(entry("etc/passwd", b'0', b"root:x:0:0\n"));
        tar.extend(entry("app/server", b'0', &[7u8; 1300]));
        tar.extend(end());

        let paths = tar_paths(tar.as_slice()).unwrap();

        assert_eq!(paths, ["/etc/", "/etc/passwd", "/app/server"]);
    }

    #[test]
    fn joins_the_ustar_prefix() {
        let mut tar = header("main.go", b'0', 0);
        tar[345..352].copy_from_slice(b"src/app");
        tar.extend(end());

        assert_eq!(tar_paths(tar.as_slice()).unwrap(), ["/src/app/main.go"]);
    }

    #[test]
    fn uses_gnu_long_names_and_pax_paths() {
        let long = format!("usr/share/{}/file.txt", "deep".repeat(40));
        let mut tar = entry("././@LongLink", b'L', format!("{long}\0").as_bytes());
        tar.extend(entry("truncated", b'0', b"x"));
        let record = "path=opt/very/long/name.bin";
        let pax = format!("{} {record}\n", record.len() + 5);
        tar.extend(entry("PaxHeaders/x", b'x', pax.as_bytes()));
        tar.extend(entry("short", b'0', b""));
        tar.extend(entry("after", b'0', b""));
        tar.extend(end());

        let paths = tar_paths(tar.as_slice()).unwrap();

        assert_eq!(
            paths,
            [
                format!("/{long}"),
                "/opt/very/long/name.bin".into(),
                "/after".into()
            ]
        );
    }

    #[test]
    fn a_truncated_stream_is_an_error() {
        let mut tar = header("big", b'0', 2000);
        tar.extend_from_slice(&[1u8; 100]);

        assert!(tar_paths(tar.as_slice()).is_err());
    }

    #[test]
    fn an_empty_stream_has_no_paths() {
        assert!(tar_paths(&[][..]).unwrap().is_empty());
    }
}

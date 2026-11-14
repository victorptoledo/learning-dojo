/// Splits a Markdown file into its YAML front matter and body.
///
/// Returns `None` when the file does not start with a `---` fenced block.
pub fn split(text: &str) -> Option<(&str, &str)> {
    let text = text.strip_prefix('\u{feff}').unwrap_or(text);
    let rest = text.strip_prefix("---")?;
    let rest = rest
        .strip_prefix("\r\n")
        .or_else(|| rest.strip_prefix('\n'))?;
    let end = find_closing_fence(rest)?;
    let yaml = &rest[..end];
    let after_fence = &rest[end + 3..];
    let body = after_fence
        .strip_prefix("\r\n")
        .or_else(|| after_fence.strip_prefix('\n'))
        .unwrap_or(after_fence);
    Some((yaml, body.trim()))
}

fn find_closing_fence(text: &str) -> Option<usize> {
    let mut offset = 0;
    for line in text.split_inclusive('\n') {
        if line.trim_end() == "---" {
            return Some(offset);
        }
        offset += line.len();
    }
    None
}

#[cfg(test)]
mod tests {
    use super::split;

    #[test]
    fn splits_yaml_and_body() {
        let (yaml, body) = split("---\nfrom: java\n---\n\nBody text.\n").unwrap();
        assert_eq!(yaml, "from: java\n");
        assert_eq!(body, "Body text.");
    }

    #[test]
    fn handles_crlf_and_empty_body() {
        let (yaml, body) = split("---\r\nfrom: java\r\n---\r\n").unwrap();
        assert_eq!(yaml, "from: java\r\n");
        assert_eq!(body, "");
    }

    #[test]
    fn rejects_files_without_front_matter() {
        assert!(split("# Just markdown").is_none());
        assert!(split("---\nfrom: java\nno closing fence").is_none());
    }
}

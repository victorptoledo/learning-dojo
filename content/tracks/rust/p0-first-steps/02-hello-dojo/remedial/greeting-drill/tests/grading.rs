use kata::{echo, initials, shout};

#[test]
fn shout_keeps_the_rest_of_the_text() {
    assert_eq!(shout("rust 2024"), "RUST 2024!");
}

#[test]
fn shout_of_nothing_is_just_the_mark() {
    assert_eq!(shout(""), "!");
}

#[test]
fn initials_ignore_extra_spaces() {
    assert_eq!(initials("  ada   lovelace "), "AL");
}

#[test]
fn initials_of_a_blank_name_are_empty() {
    assert_eq!(initials(""), "");
    assert_eq!(initials("   "), "");
}

#[test]
fn initials_handle_accented_letters() {
    assert_eq!(initials("élodie ñúñez"), "ÉÑ");
}

#[test]
fn echo_once_has_no_spaces() {
    assert_eq!(echo("hi", 1), "hi");
}

#[test]
fn echo_zero_times_is_empty() {
    assert_eq!(echo("x", 0), "");
}

#[test]
fn the_input_is_still_usable_after_the_call() {
    let name = String::from("ferris");
    let loud = shout(&name);
    assert_eq!(name, "ferris");
    assert_eq!(loud, "FERRIS!");
}

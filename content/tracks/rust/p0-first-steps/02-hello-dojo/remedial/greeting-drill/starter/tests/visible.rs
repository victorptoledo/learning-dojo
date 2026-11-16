use kata::{echo, initials, shout};

#[test]
fn shouts_a_name() {
    assert_eq!(shout("ada"), "ADA!");
}

#[test]
fn takes_initials() {
    assert_eq!(initials("grace brewster hopper"), "GBH");
}

#[test]
fn echoes_three_times() {
    assert_eq!(echo("ha", 3), "ha ha ha");
}

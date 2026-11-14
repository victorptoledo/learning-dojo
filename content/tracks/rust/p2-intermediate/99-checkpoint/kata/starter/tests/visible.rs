use kata::{GradeError, Report, Student, parse_grade_book, ranking};

#[test]
fn parses_one_student() {
    let students = parse_grade_book("Ana: 90, 80").unwrap();
    assert_eq!(students, [Student { name: "Ana".into(), scores: vec![90, 80] }]);
}

#[test]
fn reports_a_missing_colon() {
    assert_eq!(parse_grade_book("Ana 90"), Err(GradeError::MissingColon { line: 1 }));
}

#[test]
fn a_report_line_has_one_decimal_and_a_letter() {
    let ana = Student { name: "Ana".into(), scores: vec![90, 85, 100] };
    assert_eq!(ana.report(), "Ana: 91.7 (A)");
}

#[test]
fn ranking_puts_the_best_first() {
    let students = parse_grade_book("Ana: 70\nBia: 95").unwrap();
    assert_eq!(ranking(&students, 0.0), ["Bia", "Ana"]);
}

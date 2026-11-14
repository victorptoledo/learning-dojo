use kata::{GradeError, Report, Student, parse_grade_book, ranking};

const BOOK: &str = "# class 3B\nAna: 90, 85, 100\n  Bruno :70,65  \n\nCarla: 88\nDavi: 88\n";

fn student(name: &str, scores: &[u32]) -> Student {
    Student { name: name.into(), scores: scores.to_vec() }
}

#[test]
fn skips_comments_and_blank_lines_and_trims_everything() {
    let students = parse_grade_book(BOOK).unwrap();
    assert_eq!(
        students,
        [
            student("Ana", &[90, 85, 100]),
            student("Bruno", &[70, 65]),
            student("Carla", &[88]),
            student("Davi", &[88]),
        ]
    );
}

#[test]
fn an_empty_book_has_no_students() {
    assert_eq!(parse_grade_book(""), Ok(vec![]));
    assert_eq!(parse_grade_book("# nothing yet\n\n"), Ok(vec![]));
}

#[test]
fn line_numbers_count_skipped_lines() {
    assert_eq!(
        parse_grade_book("# header\n\nAna: 90\n: 80"),
        Err(GradeError::MissingName { line: 4 })
    );
}

#[test]
fn a_line_without_scores_is_an_error() {
    assert_eq!(parse_grade_book("Ana:   "), Err(GradeError::NoScores { line: 1 }));
}

#[test]
fn bad_scores_report_the_trimmed_text() {
    assert_eq!(
        parse_grade_book("Ana: 90, abc"),
        Err(GradeError::BadScore { line: 1, value: "abc".into() })
    );
    assert_eq!(
        parse_grade_book("Ana: 101"),
        Err(GradeError::BadScore { line: 1, value: "101".into() })
    );
    assert_eq!(
        parse_grade_book("Ana: -5"),
        Err(GradeError::BadScore { line: 1, value: "-5".into() })
    );
    assert_eq!(
        parse_grade_book("Ana: 90,"),
        Err(GradeError::BadScore { line: 1, value: "".into() })
    );
}

#[test]
fn the_first_problem_wins() {
    assert_eq!(parse_grade_book("Ana 90\n: 80"), Err(GradeError::MissingColon { line: 1 }));
}

#[test]
fn error_messages_are_exact() {
    assert_eq!(GradeError::MissingColon { line: 2 }.to_string(), "line 2: missing ':'");
    assert_eq!(GradeError::MissingName { line: 2 }.to_string(), "line 2: missing name");
    assert_eq!(GradeError::NoScores { line: 2 }.to_string(), "line 2: no scores");
    assert_eq!(
        GradeError::BadScore { line: 2, value: "abc".into() }.to_string(),
        "line 2: 'abc' is not a score from 0 to 100"
    );
}

#[test]
fn grade_error_is_a_standard_error() {
    let error: Box<dyn std::error::Error> = Box::new(GradeError::NoScores { line: 1 });
    assert_eq!(error.to_string(), "line 1: no scores");
}

#[test]
fn letters_follow_the_average_bands() {
    let letter = |score| student("x", &[score]).letter();
    assert_eq!(letter(100), 'A');
    assert_eq!(letter(90), 'A');
    assert_eq!(letter(89), 'B');
    assert_eq!(letter(80), 'B');
    assert_eq!(letter(70), 'C');
    assert_eq!(letter(60), 'D');
    assert_eq!(letter(59), 'F');
    assert_eq!(letter(0), 'F');
}

#[test]
fn averages_are_not_truncated() {
    assert!((student("x", &[70, 65]).average() - 67.5).abs() < 1e-9);
    assert_eq!(student("Bruno", &[70, 65]).report(), "Bruno: 67.5 (D)");
    assert_eq!(student("Ana", &[89, 90]).report(), "Ana: 89.5 (B)");
}

#[test]
fn ranking_filters_sorts_and_breaks_ties_by_name() {
    let students = parse_grade_book(BOOK).unwrap();
    assert_eq!(ranking(&students, 80.0), ["Ana", "Carla", "Davi"]);
    assert_eq!(ranking(&students, 88.0), ["Ana", "Carla", "Davi"]);
    assert_eq!(ranking(&students, 0.0), ["Ana", "Carla", "Davi", "Bruno"]);
    assert!(ranking(&students, 99.0).is_empty());
}

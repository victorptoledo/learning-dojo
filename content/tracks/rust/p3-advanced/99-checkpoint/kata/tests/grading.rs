use kata::{Build, Job, ParseError, PlanError, Report, Target, parse, plan, start};
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::{Arc, Mutex, mpsc};
use std::thread;
use std::time::{Duration, Instant};

const BUILD_FILE: &str = "\
# a small workspace
core:
  utils :   core

app: utils core

docs:
lint: core
";

fn owned(waves: &[Vec<&str>]) -> Vec<Vec<String>> {
    waves.iter().map(|wave| wave.iter().map(|name| name.to_string()).collect()).collect()
}

fn names(list: &[&str]) -> Vec<String> {
    list.iter().map(|name| name.to_string()).collect()
}

fn job(f: impl Fn(&str) -> Result<u64, String> + Send + Sync + 'static) -> Job {
    Arc::new(f)
}

/// Waits for the report on another thread, so a build that never ends fails the test
/// instead of hanging the whole run.
fn wait_within(build: Build, seconds: u64) -> Report {
    let (sender, receiver) = mpsc::channel();
    thread::spawn(move || {
        let _ = sender.send(build.wait());
    });
    receiver
        .recv_timeout(Duration::from_secs(seconds))
        .expect("the build did not finish in time")
}

fn eventually(seconds: u64, condition: impl Fn() -> bool) -> bool {
    let deadline = Instant::now() + Duration::from_secs(seconds);
    while Instant::now() < deadline {
        if condition() {
            return true;
        }
        thread::sleep(Duration::from_millis(5));
    }
    condition()
}

// ---- Part 1: parse ------------------------------------------------------------------------

#[test]
fn parses_targets_in_order_and_trims_everything() {
    assert_eq!(
        parse(BUILD_FILE).unwrap(),
        [
            Target { name: "core", deps: vec![] },
            Target { name: "utils", deps: vec!["core"] },
            Target { name: "app", deps: vec!["utils", "core"] },
            Target { name: "docs", deps: vec![] },
            Target { name: "lint", deps: vec!["core"] },
        ]
    );
}

#[test]
fn every_name_points_into_the_source() {
    let range = BUILD_FILE.as_bytes().as_ptr_range();
    let inside = |text: &str| range.contains(&text.as_ptr());
    for target in parse(BUILD_FILE).unwrap() {
        assert!(inside(target.name), "`{}` was copied out of the source", target.name);
        for dep in target.deps {
            assert!(inside(dep), "`{dep}` was copied out of the source");
        }
    }
}

#[test]
fn a_dependency_listed_twice_is_kept_once() {
    let targets = parse("a:\nb: a a  a\n").unwrap();
    assert_eq!(targets[1].deps, ["a"]);
}

#[test]
fn an_empty_source_has_no_targets() {
    assert_eq!(parse(""), Ok(vec![]));
    assert_eq!(parse("# only comments\n\n   \n"), Ok(vec![]));
}

#[test]
fn errors_carry_the_line_number_counting_skipped_lines() {
    assert_eq!(parse("# header\n\na:\nb a\n"), Err(ParseError::MissingColon { line: 4 }));
    assert_eq!(parse("a:\n   : a\n"), Err(ParseError::EmptyName { line: 2 }));
    assert_eq!(
        parse("a:\nb: a\n\n a : b\n"),
        Err(ParseError::Duplicate { line: 4, name: "a".into() })
    );
}

#[test]
fn the_first_problem_wins() {
    assert_eq!(parse("a b\n: c\n"), Err(ParseError::MissingColon { line: 1 }));
}

#[test]
fn parse_errors_have_readable_messages() {
    assert_eq!(ParseError::MissingColon { line: 3 }.to_string(), "line 3: missing ':'");
    assert_eq!(ParseError::EmptyName { line: 3 }.to_string(), "line 3: empty target name");
    assert_eq!(
        ParseError::Duplicate { line: 4, name: "app".into() }.to_string(),
        "line 4: target `app` is defined twice"
    );
}

// ---- Part 2: plan -------------------------------------------------------------------------

#[test]
fn waves_respect_dependencies_and_are_sorted() {
    let targets = parse(BUILD_FILE).unwrap();
    assert_eq!(
        plan(&targets).unwrap(),
        [vec!["core", "docs"], vec!["lint", "utils"], vec!["app"]]
    );
}

#[test]
fn the_plan_outlives_the_target_list() {
    let waves = {
        let targets = parse(BUILD_FILE).unwrap();
        plan(&targets).unwrap()
    };
    assert_eq!(waves.concat().len(), 5);
}

#[test]
fn a_target_is_placed_in_the_first_wave_it_can_run_in() {
    let targets = parse("d: c\nc: b\nb: a\na:\nx: a\n").unwrap();
    assert_eq!(
        plan(&targets).unwrap(),
        [vec!["a"], vec!["b", "x"], vec!["c"], vec!["d"]]
    );
}

#[test]
fn an_unknown_dependency_is_reported() {
    let targets = parse("a:\nb: a missing\n").unwrap();
    assert_eq!(
        plan(&targets),
        Err(PlanError::UnknownDependency { target: "b".into(), dependency: "missing".into() })
    );
}

#[test]
fn a_cycle_lists_every_target_that_cannot_be_scheduled() {
    let targets = parse("ok:\nzeta: after\nc: b ok\nb: c\nafter: b\nalpha: c ok\n").unwrap();
    assert_eq!(
        plan(&targets),
        Err(PlanError::Cycle { targets: names(&["after", "alpha", "b", "c", "zeta"]) })
    );
}

#[test]
fn a_target_that_depends_on_itself_is_a_cycle() {
    let targets = parse("self: self\n").unwrap();
    assert_eq!(plan(&targets), Err(PlanError::Cycle { targets: names(&["self"]) }));
}

#[test]
fn plan_errors_have_readable_messages() {
    let unknown = PlanError::UnknownDependency { target: "b".into(), dependency: "x".into() };
    assert_eq!(unknown.to_string(), "`b` depends on unknown target `x`");
    let cycle = PlanError::Cycle { targets: names(&["b", "c"]) };
    assert_eq!(cycle.to_string(), "dependency cycle among b, c");
}

#[test]
fn an_empty_list_has_an_empty_plan() {
    assert_eq!(plan(&[]), Ok(vec![]));
}

// ---- Part 3: build ------------------------------------------------------------------------

#[test]
fn builds_every_target_and_sorts_the_report() {
    let waves = owned(&plan(&parse(BUILD_FILE).unwrap()).unwrap());
    let report = wait_within(start(waves, 2, job(|name| Ok(name.len() as u64))), 10);
    assert_eq!(
        report,
        Report {
            built: vec![
                ("app".into(), 3),
                ("core".into(), 4),
                ("docs".into(), 4),
                ("lint".into(), 4),
                ("utils".into(), 5),
            ],
            failed: vec![],
            skipped: vec![],
        }
    );
}

#[test]
fn start_returns_before_the_work_is_done() {
    let begin = Instant::now();
    let build = start(
        vec![names(&["slow"])],
        1,
        job(|_| {
            thread::sleep(Duration::from_millis(400));
            Ok(1)
        }),
    );
    assert!(begin.elapsed() < Duration::from_millis(200), "start blocked for {:?}", begin.elapsed());
    assert_eq!(wait_within(build, 10).built, [("slow".to_string(), 1)]);
}

#[test]
fn a_wave_runs_on_several_threads() {
    let begin = Instant::now();
    let build = start(
        vec![names(&["a", "b", "c", "d"])],
        4,
        job(|_| {
            thread::sleep(Duration::from_millis(300));
            Ok(0)
        }),
    );
    assert_eq!(wait_within(build, 10).built.len(), 4);
    let took = begin.elapsed();
    assert!(took < Duration::from_millis(900), "four 300 ms jobs on four workers took {took:?}");
}

#[test]
fn never_more_jobs_at_once_than_workers() {
    let running = Arc::new(AtomicUsize::new(0));
    let peak = Arc::new(AtomicUsize::new(0));
    let (now, top) = (Arc::clone(&running), Arc::clone(&peak));
    let build = start(
        vec![names(&["a", "b", "c", "d", "e", "f", "g", "h"])],
        3,
        job(move |_| {
            let current = now.fetch_add(1, Ordering::SeqCst) + 1;
            top.fetch_max(current, Ordering::SeqCst);
            thread::sleep(Duration::from_millis(60));
            now.fetch_sub(1, Ordering::SeqCst);
            Ok(0)
        }),
    );
    assert_eq!(wait_within(build, 10).built.len(), 8);
    let peak = peak.load(Ordering::SeqCst);
    assert!(peak <= 3, "{peak} jobs ran at once with 3 workers");
    assert!(peak >= 2, "the jobs never overlapped: the wave ran on one thread");
}

#[test]
fn zero_workers_means_one() {
    let report = wait_within(start(vec![names(&["a", "b"])], 0, job(|_| Ok(7))), 10);
    assert_eq!(report.built, [("a".to_string(), 7), ("b".to_string(), 7)]);
}

#[test]
fn a_wave_starts_only_after_the_previous_one_finished() {
    let log = Arc::new(Mutex::new(Vec::new()));
    let events = Arc::clone(&log);
    let build = start(
        vec![names(&["fast", "slow"]), names(&["next"])],
        3,
        job(move |name| {
            events.lock().unwrap().push(format!("start {name}"));
            if name == "slow" {
                thread::sleep(Duration::from_millis(200));
            }
            events.lock().unwrap().push(format!("end {name}"));
            Ok(0)
        }),
    );
    wait_within(build, 10);
    let log = log.lock().unwrap();
    let position = |event: &str| log.iter().position(|e| e == event).unwrap();
    assert!(position("start next") > position("end slow"), "{log:?}");
    assert!(position("start next") > position("end fast"), "{log:?}");
}

#[test]
fn a_failure_finishes_its_wave_and_skips_the_rest() {
    let waves = vec![names(&["a", "b", "c"]), names(&["d", "e"]), names(&["f"])];
    let report = wait_within(
        start(waves, 1, job(|name| if name == "a" { Err("disk full".into()) } else { Ok(1) })),
        10,
    );
    assert_eq!(report.built, [("b".to_string(), 1), ("c".to_string(), 1)]);
    assert_eq!(report.failed, [("a".to_string(), "disk full".to_string())]);
    assert_eq!(report.skipped, names(&["d", "e", "f"]));
}

#[test]
fn a_job_that_panics_is_a_failure() {
    let waves = vec![names(&["boom", "fine"]), names(&["later"])];
    let report = wait_within(
        start(waves, 2, job(|name| if name == "boom" { panic!("kaboom") } else { Ok(2) })),
        10,
    );
    assert_eq!(report.built, [("fine".to_string(), 2)]);
    assert_eq!(report.failed, [("boom".to_string(), "panicked".to_string())]);
    assert_eq!(report.skipped, names(&["later"]));
}

/// A job that reports when it starts and then waits for the test to let it go.
fn gated() -> (Job, mpsc::Receiver<String>, mpsc::Sender<()>) {
    let (started, started_rx) = mpsc::channel::<String>();
    let (release, release_rx) = mpsc::channel::<()>();
    let started = Mutex::new(started);
    let release_rx = Mutex::new(release_rx);
    let job = job(move |name| {
        started.lock().unwrap().send(name.to_string()).unwrap();
        release_rx.lock().unwrap().recv_timeout(Duration::from_secs(5)).ok();
        Ok(0)
    });
    (job, started_rx, release)
}

#[test]
fn finished_counts_jobs_while_the_build_runs() {
    let (job, started, release) = gated();
    let build = start(vec![names(&["a", "b"])], 1, job);
    assert_eq!(started.recv_timeout(Duration::from_secs(5)).unwrap(), "a");
    assert_eq!(build.finished(), 0);
    release.send(()).unwrap();
    assert_eq!(started.recv_timeout(Duration::from_secs(5)).unwrap(), "b");
    assert!(eventually(5, || build.finished() == 1), "finished() is {}", build.finished());
    release.send(()).unwrap();
    assert!(eventually(5, || build.finished() == 2), "finished() is {}", build.finished());
    assert_eq!(wait_within(build, 10).built.len(), 2);
}

#[test]
fn cancel_lets_running_jobs_finish_and_skips_the_others() {
    let (job, started, release) = gated();
    let build = start(vec![names(&["a", "b", "c"]), names(&["d"])], 1, job);
    assert_eq!(started.recv_timeout(Duration::from_secs(5)).unwrap(), "a");
    build.cancel();
    release.send(()).unwrap();
    let report = wait_within(build, 10);
    assert_eq!(report.built, [("a".to_string(), 0)]);
    assert_eq!(report.skipped, names(&["b", "c", "d"]));
    assert!(started.try_recv().is_err(), "a job started after cancel");
}

#[test]
fn an_empty_plan_gives_an_empty_report() {
    assert_eq!(wait_within(start(vec![], 4, job(|_| Ok(0))), 10), Report::default());
}

#[test]
fn a_build_can_be_moved_to_another_thread() {
    fn assert_send<T: Send + 'static>() {}
    assert_send::<Build>();
}

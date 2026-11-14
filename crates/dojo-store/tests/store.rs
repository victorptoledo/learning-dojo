use chrono::NaiveDate;
use dojo_core::{
    Archetype, Catalog, ContentStatus, Node, NodeKind, Phase, Profile, ProgressEvent,
    ProgressStore, Settings, StreakBaseline, Track,
};
use dojo_store::{ImportOutcome, SqliteStore, import_v1_profile};
use std::collections::BTreeMap;
use std::fs;

fn day(d: u32) -> NaiveDate {
    NaiveDate::from_ymd_opt(2026, 11, d).unwrap()
}

fn completed(node_id: &str, on: NaiveDate) -> ProgressEvent {
    ProgressEvent::NodeCompleted {
        track_id: "rust".into(),
        node_id: node_id.into(),
        xp: 100,
        on,
    }
}

fn catalog() -> Catalog {
    let node = |id: &str, legacy: &str| Node {
        id: id.into(),
        legacy_id: Some(legacy.into()),
        kind: NodeKind::Lesson,
        status: ContentStatus::Draft,
        title: id.into(),
        summary: String::new(),
        objectives: vec![],
        prerequisites: vec![],
        difficulty: 1,
        estimated_minutes: 10,
        xp: 150,
        lesson: None,
        bridges: BTreeMap::new(),
        kata: None,
    };
    let phase = Phase {
        id: "p0".into(),
        order: 0,
        title: "First Steps".into(),
        goal: String::new(),
        nodes: vec![
            node("rust.p0.setup", "rust-01-setup"),
            node("rust.p0.hello", "rust-02-hello"),
        ],
    };
    Catalog {
        tracks: vec![Track {
            id: "rust".into(),
            name: "Rust".into(),
            archetype: Archetype::Language,
            category: "Programming Languages".into(),
            icon: "🦀".into(),
            description: String::new(),
            status: ContentStatus::Draft,
            belts: vec![],
            phases: vec![phase],
        }],
        skills: BTreeMap::new(),
    }
}

#[test]
fn new_store_has_no_settings_and_no_events() {
    let store = SqliteStore::open_in_memory().unwrap();

    assert!(store.load_settings().unwrap().is_none());
    assert!(store.load_events().unwrap().is_empty());
    assert_eq!(store.schema_version().unwrap(), 2);
}

#[test]
fn settings_round_trip_including_streak_baseline() {
    let mut store = SqliteStore::open_in_memory().unwrap();
    let mut settings = Settings::new("docker");
    settings.name = "Victor".into();
    settings.bridge_from = "python".into();
    settings.streak_baseline = StreakBaseline {
        current: 3,
        best: 7,
        last_trained: Some(day(13)),
    };

    store.save_settings(&settings).unwrap();
    assert_eq!(store.load_settings().unwrap(), Some(settings.clone()));

    settings.streak_baseline.last_trained = None;
    store.save_settings(&settings).unwrap();
    assert_eq!(
        store
            .load_settings()
            .unwrap()
            .unwrap()
            .streak_baseline
            .last_trained,
        None
    );
}

#[test]
fn events_are_returned_in_recording_order_and_rebuild_the_profile() {
    let mut store = SqliteStore::open_in_memory().unwrap();
    let reset = ProgressEvent::NodeReset {
        track_id: "rust".into(),
        node_id: "a".into(),
        xp: 100,
        on: day(14),
    };
    let events = [completed("a", day(13)), completed("b", day(14)), reset];
    for event in &events {
        store.append_event(event).unwrap();
    }

    let loaded = store.load_events().unwrap();
    assert_eq!(loaded, events);

    let profile = Profile::replay(Settings::new("rust"), &loaded);
    assert!(!profile.is_completed("rust", "a"));
    assert!(profile.is_completed("rust", "b"));
    assert_eq!(profile.total_xp, 100);
}

#[test]
fn data_survives_reopening_the_file() {
    let dir = tempfile::tempdir().unwrap();
    let path = dir.path().join("nested/dojo.db");
    {
        let mut store = SqliteStore::open(&path).unwrap();
        store.save_settings(&Settings::new("rust")).unwrap();
        store.append_event(&completed("a", day(14))).unwrap();
    }

    let reopened = SqliteStore::open(&path).unwrap();

    assert_eq!(reopened.load_events().unwrap().len(), 1);
    assert_eq!(
        reopened.load_settings().unwrap().unwrap().active_track_id,
        "rust"
    );
}

#[test]
fn v1_profile_is_imported_once_with_legacy_ids_mapped_and_file_renamed() {
    let dir = tempfile::tempdir().unwrap();
    let v1_path = dir.path().join("profile.json");
    fs::write(
        &v1_path,
        r#"{"name":"Dev Sensei","total_xp":250,"current_streak":4,"best_streak":6,
           "last_trained":"2026-11-13","active_track_id":"rust",
           "track_progress":{"rust":["rust-01-setup","rust-99-removed"]}}"#,
    )
    .unwrap();
    let mut store = SqliteStore::open_in_memory().unwrap();

    let outcome = import_v1_profile(&v1_path, &catalog(), &mut store, day(14)).unwrap();

    assert_eq!(
        outcome,
        ImportOutcome::Imported {
            completed_nodes: 1,
            unknown_nodes: 1,
            backup: dir.path().join("profile.v1.json")
        }
    );
    assert!(!v1_path.exists());
    let settings = store.load_settings().unwrap().unwrap();
    let profile = Profile::replay(settings, &store.load_events().unwrap());
    assert!(profile.is_completed("rust", "rust.p0.setup"));
    assert_eq!(
        profile.total_xp, 150,
        "XP comes from the current catalog, not the v1 total"
    );
    assert_eq!(profile.streak_on(day(14)), 4);
    assert_eq!(profile.best_streak, 6);

    assert_eq!(
        import_v1_profile(&v1_path, &catalog(), &mut store, day(14)).unwrap(),
        ImportOutcome::NothingToImport
    );
}

#[test]
fn import_is_skipped_when_the_store_already_has_a_profile() {
    let dir = tempfile::tempdir().unwrap();
    let v1_path = dir.path().join("profile.json");
    fs::write(&v1_path, r#"{"name":"x","current_streak":1,"best_streak":1,"last_trained":null,"active_track_id":"rust"}"#)
        .unwrap();
    let mut store = SqliteStore::open_in_memory().unwrap();
    store.save_settings(&Settings::new("docker")).unwrap();

    let outcome = import_v1_profile(&v1_path, &catalog(), &mut store, day(14)).unwrap();

    assert_eq!(outcome, ImportOutcome::NothingToImport);
    assert!(v1_path.exists(), "the v1 file is left untouched");
}

#[test]
fn check_history_keeps_every_attempt_and_returns_the_latest() {
    use chrono::{TimeZone, Utc};
    use dojo_core::{CheckOutcome, CheckRecord};
    let mut store = SqliteStore::open_in_memory().unwrap();
    let attempt = |outcome, passed, minute| CheckRecord {
        track_id: "rust".into(),
        node_id: "rust.p0.hello-dojo".into(),
        outcome,
        passed_tests: passed,
        total_tests: 5,
        duration_ms: 1200,
        ran_at: Utc.with_ymd_and_hms(2026, 11, 14, 12, minute, 0).unwrap(),
    };
    assert_eq!(
        store.latest_check("rust", "rust.p0.hello-dojo").unwrap(),
        None
    );

    store
        .append_check(&attempt(CheckOutcome::CompileError, 0, 1))
        .unwrap();
    store
        .append_check(&attempt(CheckOutcome::Passed, 5, 2))
        .unwrap();

    assert_eq!(store.check_count("rust", "rust.p0.hello-dojo").unwrap(), 2);
    assert_eq!(
        store.latest_check("rust", "rust.p0.hello-dojo").unwrap(),
        Some(attempt(CheckOutcome::Passed, 5, 2))
    );
    assert_eq!(store.check_count("rust", "other").unwrap(), 0);
}

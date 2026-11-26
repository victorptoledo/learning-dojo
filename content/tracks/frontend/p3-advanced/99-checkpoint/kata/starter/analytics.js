// A small first-party analytics snippet. Done: do not change. It needs no other script and
// nothing waits for it, so it must not hold up the page.
window.analytics = { events: [] };
window.analytics.track = (name) => window.analytics.events.push({ name, at: Date.now() });
window.analytics.track("page_view");

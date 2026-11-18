When a classic web page loads, the browser names the tab, puts the reader at the top of the
new page and screen readers announce it. A single page app does none of that unless you do
it. Do it:

- **The tab title** is `<page heading> · Help desk` on every page (`Overview · Help desk`,
  `Cannot log in · Help desk`, `Page not found · Help desk`), from the first page on.
- **Focus.** After a navigation (a link or Back and Forward), the focus moves to the new
  page's level 1 heading, which takes it with `tabIndex={-1}` (so it is not a Tab stop).
  On the page the app opens on, the focus is left alone.
- **Announcement.** The app has a `role="status"` live region that says
  `Navigated to <page heading>` after each navigation, and nothing on the first page.
- **Skip link.** The first link of the page, before the navigation, is `Skip to content`
  (`href="#main"`), and it moves the focus to the `<main id="main">` element, which holds the
  page. Keyboard users press Tab once and skip the menu.

For a ticket page, the heading is the ticket's title, so the focus and the announcement
happen once the ticket is there.

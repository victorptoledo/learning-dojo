A router of your own, on the History API, and the pages it shows. Export `HelpDesk` (props:
`api`) from `HelpDesk.tsx`.

**The ticket list** is loaded once, with `api.listTickets()`, when the app starts, and shared
by every page: moving between pages never loads it again. While it loads, the pages that need
it say `Loading tickets`; when it fails they show an `alert` with `Could not load the
tickets.` and a `Try again` button that loads it again.

**Pages.** Each one has its title as its only level 1 heading:

| Path | Heading | Content |
|---|---|---|
| `/` | `Overview` | a list named `Tickets by status`: `Open: 2`, `Pending: 1`, `Closed: 1` |
| `/tickets` | `Tickets` | a list named `Tickets`, newest `updatedAt` first: per ticket a link to `/tickets/<id>` with its title, and `<Status> · <Priority> · <requester>` (`Closed · High · Bia`) |
| `/tickets/<id>` | the ticket's title | a description list (`<dl>`) with `Status`, `Priority` and `Requester` (`<dt>Status</dt><dd>Pending</dd>`), and a link `All tickets` |
| `/tickets/<unknown id>` | `Ticket not found` | a link `All tickets` |
| anything else | `Page not found` | a link `Go to the overview` |

**Navigation.** A `<nav>` named `Main` with two links, `Overview` (`/`) and `Tickets`
(`/tickets`); the link of the current section has `aria-current="page"` (a ticket is in the
Tickets section; a page not found is in none).

**Links** are real `<a href>` elements, so they can be copied, hovered and opened in a new
tab. A plain click changes the page and pushes a history entry without reloading
(`history.pushState`); a click with Ctrl, Shift, Meta or Alt is left to the browser. The app
starts on the page of the URL it is opened on, and the browser's Back and Forward buttons
(`popstate`) bring the pages back.

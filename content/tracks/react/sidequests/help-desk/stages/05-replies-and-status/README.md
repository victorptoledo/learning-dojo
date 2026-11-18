Support agents answer tickets and close them, and they will not wait for a spinner each
time. Both changes show at once and roll back when the server says no.

**Replies.** Under the messages, a form named `Reply` with a text area labelled `Your reply`
and a `Send reply` button.

- An empty (or blank) reply is not sent: the text area gets `aria-invalid="true"`, points
  with `aria-describedby` at the message `Write a reply first`, and takes the focus.
- Sending calls `api.reply(id, text)` with the trimmed text and, at once, adds the reply at
  the end of the messages with the author `You` and the word `Sending…`; the text area is
  emptied and `Send reply` is disabled until the server answers.
- When the server answers, the message it returns replaces the one that was sending.
- When it fails, the sending message goes away, an `alert` says `Your reply was not sent.`,
  and the text is back in the text area, so nothing typed is lost.
- A stored reply is still there when the ticket is opened again, without loading it again.

`useOptimistic` was made for this, inside a transition (`useTransition` or a form action
with `useActionState`).

**Status.** A select labelled `Change status` (`Open`, `Pending`, `Closed`) shows the
ticket's status. Choosing another one calls `api.setStatus(id, status)` and, without waiting,
shows it everywhere: the ticket page, the `Tickets` list and the counts on the `Overview`.
When the server refuses, the old status comes back everywhere and an `alert` says
`Could not change the status.` A stored status is kept when the ticket is opened again.

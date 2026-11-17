"""Helpers shared by the contract tests: feed documents and a fake fetch."""

import asyncio


def rss(*items: str) -> str:
    return f'<?xml version="1.0"?><rss version="2.0"><channel><title>T</title>{"".join(items)}</channel></rss>'


def rss_item(title: str | None, link: str | None, date: str | None = None) -> str:
    parts = []
    if title is not None:
        parts.append(f"<title>{title}</title>")
    if link is not None:
        parts.append(f"<link>{link}</link>")
    if date is not None:
        parts.append(f"<pubDate>{date}</pubDate>")
    return f"<item>{''.join(parts)}</item>"


def atom(*entries: str) -> str:
    return f'<?xml version="1.0"?><feed xmlns="http://www.w3.org/2005/Atom"><title>A</title>{"".join(entries)}</feed>'


class FakeFetch:
    """Answers each URL from a table: text, an exception to raise, or a list of those (one per
    attempt). `delays` makes a URL sleep first. Counts calls and the most at once."""

    def __init__(self, answers: dict, delays: dict | None = None) -> None:
        self.answers = answers
        self.delays = delays or {}
        self.calls: dict[str, int] = {}
        self.running = 0
        self.most_at_once = 0

    async def __call__(self, url: str) -> str:
        self.calls[url] = self.calls.get(url, 0) + 1
        self.running += 1
        self.most_at_once = max(self.most_at_once, self.running)
        try:
            delay = self.delays.get(url, 0)
            if isinstance(delay, list):
                delay = delay[min(self.calls[url], len(delay)) - 1]
            await asyncio.sleep(delay)
            answer = self.answers[url]
            if isinstance(answer, list):
                answer = answer[min(self.calls[url], len(answer)) - 1]
            if isinstance(answer, BaseException):
                raise answer
            return answer
        finally:
            self.running -= 1

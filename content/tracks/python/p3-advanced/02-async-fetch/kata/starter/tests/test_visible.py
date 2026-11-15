import asyncio

from fetcher import Outcome, fetch_all


async def fake_fetch(url: str) -> str:
    await asyncio.sleep(0.01)
    return f"body of {url}"


def test_fetches_every_url():
    outcomes = asyncio.run(fetch_all(["a", "b"], fake_fetch))
    assert outcomes == [Outcome("a", body="body of a"), Outcome("b", body="body of b")]

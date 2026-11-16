# Resilient Gateway: Retries, Rate Limits, Caching and Fallback

Teams put a small gateway in front of their model calls so that every feature gets the same
retries, rate limiting, caching, fallback and cost accounting. You build one on a fake clock and a
fake random function, which is also how you make retry logic testable without waiting. Press
**Enter** to create the kata folder and open it.

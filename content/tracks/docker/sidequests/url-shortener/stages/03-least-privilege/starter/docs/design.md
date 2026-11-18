# Shortener design notes

- codes are the first 7 characters of the URL-safe base64 of the URL's SHA-256
- links live in memory; persistence is a later project
- the admin token for production is in deploy/production.env

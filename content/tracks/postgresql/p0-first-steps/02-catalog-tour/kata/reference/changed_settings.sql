SELECT name, setting, unit, source
FROM pg_settings
WHERE source NOT IN ('default', 'override')
ORDER BY name;

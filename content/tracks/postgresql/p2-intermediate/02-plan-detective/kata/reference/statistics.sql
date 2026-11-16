-- The city decides the country, but the planner multiplies their selectivities as if they
-- were independent. A multi-column MCV list stores the real combinations, so impossible pairs
-- are estimated low too (dependencies alone would not catch Porto in ES).
CREATE STATISTICS addresses_city_country (dependencies, mcv) ON city, country FROM addresses;

-- Extended statistics are only filled by the next ANALYZE.
ANALYZE addresses;

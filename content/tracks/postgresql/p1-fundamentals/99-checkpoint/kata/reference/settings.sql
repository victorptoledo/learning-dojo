CREATE ROLE booking_api LOGIN;
ALTER ROLE booking_api SET statement_timeout = '5s';
ALTER ROLE booking_api SET idle_in_transaction_session_timeout = '1min';

ALTER TABLE bookings SET (autovacuum_vacuum_scale_factor = 0.02, autovacuum_analyze_scale_factor = 0.01);

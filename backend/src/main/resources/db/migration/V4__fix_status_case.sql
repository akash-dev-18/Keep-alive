UPDATE monitors SET status = 'unknown' WHERE status = 'UNKNOWN';
ALTER TABLE monitors ALTER COLUMN status SET DEFAULT 'unknown';

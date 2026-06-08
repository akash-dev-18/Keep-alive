ALTER TABLE monitors ALTER COLUMN check_interval_min SET DEFAULT 2;
UPDATE monitors SET check_interval_min = 2 WHERE check_interval_min = 5;

-- Complete the requested resource identity reset; keep shared revisions and UI layouts.
-- Existing resource records and resource audit history are deliberately discarded.
TRUNCATE TABLE seasons_audit, seasons, tournaments_audit, tournaments;

ALTER TABLE tournaments RENAME COLUMN short_id TO nano_id;
ALTER TABLE tournaments ALTER COLUMN nano_id TYPE varchar(4);
ALTER TABLE tournaments RENAME CONSTRAINT tournaments_short_id_key TO tournaments_nano_id_key;
ALTER TABLE tournaments_audit RENAME COLUMN short_id TO nano_id;
ALTER TABLE tournaments_audit ALTER COLUMN nano_id TYPE varchar(4);

ALTER TABLE seasons RENAME COLUMN short_id TO nano_id;
ALTER TABLE seasons ALTER COLUMN nano_id TYPE varchar(4);
ALTER TABLE seasons RENAME CONSTRAINT seasons_short_id_key TO seasons_nano_id_key;
ALTER TABLE seasons_audit RENAME COLUMN short_id TO nano_id;
ALTER TABLE seasons_audit ALTER COLUMN nano_id TYPE varchar(4);

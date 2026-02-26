-- Database initialization for Event Processing & Analytics Platform

CREATE TABLE IF NOT EXISTS events_raw (
  id BIGSERIAL PRIMARY KEY,
  event_type TEXT NOT NULL,
  user_id TEXT NOT NULL,
  session_id TEXT,
  ts TIMESTAMPTZ NOT NULL,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE IF NOT EXISTS event_agg_daily (
  day DATE NOT NULL,
  event_type TEXT NOT NULL,
  event_count BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (day, event_type)
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_events_raw_ts ON events_raw(ts);
CREATE INDEX IF NOT EXISTS idx_events_raw_type_ts ON events_raw(event_type, ts);

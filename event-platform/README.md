Event Processing & Analytics Platform

A production-style, containerized event streaming system demonstrating hybrid Software Engineering + Data Engineering capabilities.

This project simulates a real-world analytics pipeline where user events are ingested in real-time, streamed through Kafka, processed in batches, stored in PostgreSQL, and exposed via analytics APIs.

What This Project Demonstrates
Real-time REST ingestion (Spring Boot)
Distributed streaming with Kafka (multi-partition topic)
Consumer group batch processing
Idempotent upsert aggregation logic
Normalized relational storage + JSONB metadata
Optimized SQL-backed analytics endpoints
Fully Dockerized local environment (one-command startup)
Separation of concerns (producer vs consumer services)

Architecture
Client
  │
  │  POST /events
  ▼
producer-api (Spring Boot REST)
  │
  │  Kafka Producer
  ▼
Kafka topic: events.v1 (3 partitions)
  │
  │  Consumer Group: event-consumers (batch listener)
  ▼
consumer-worker (Spring Boot)
  │
  ├─ INSERT → events_raw (JSONB metadata)
  └─ UPSERT → event_agg_daily (daily counts)
  ▼
PostgreSQL 16
  ▲
  │  GET /analytics/events/daily
  │
producer-api

Design Highlights
Event Flow
Client sends JSON event to POST /events
Producer publishes event to Kafka topic (events.v1)
Consumer group processes events in batches
Raw events stored in events_raw
Aggregated daily counts upserted into event_agg_daily
Analytics endpoints serve pre-aggregated data
Why Batch Processing?
Batch consumption:
Improves throughput
Reduces DB round trips
Simulates real-world stream processing optimization

Why JSONB?
Flexible schema for event metadata
Queryable structured storage
Common real-world analytics pattern

Tech Stack
Java 17
Spring Boot 3
Spring Kafka
PostgreSQL 16
Kafka (Confluent Images)
Docker + Docker Compose

⚡ Quick Start
1️⃣ Start the Platform
docker compose up --build

Services exposed:

API: http://localhost:8080

Kafka (host access): localhost:29092

Postgres: localhost:5432

2️⃣ Send Events
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "PAGE_VIEW",
    "userId": "u123",
    "sessionId": "s888",
    "timestamp": "2026-02-26T12:00:00Z",
    "metadata": {"path": "/pricing", "ref": "google"}
  }'

Send multiple events (vary eventType, userId, timestamps).

3️⃣ Query Analytics
curl "http://localhost:8080/analytics/events/daily?from=2026-02-01&to=2026-03-01"

Example response:

[
  {"day":"2026-02-26","eventType":"PAGE_VIEW","eventCount":12},
  {"day":"2026-02-26","eventType":"PURCHASE","eventCount":2}
]
🗄 Data Model

Tables initialized via sql/init.sql:

events_raw
Column	Type
event_type	TEXT
user_id	TEXT
session_id	TEXT
ts	TIMESTAMP
metadata	JSONB
event_agg_daily
Column	Type
day	DATE
event_type	TEXT
event_count	BIGINT
(day, event_type)	PRIMARY KEY

Aggregation maintained via:

ON CONFLICT (day, event_type)
DO UPDATE SET event_count = event_agg_daily.event_count + EXCLUDED.event_count
📂 Project Structure
event-platform/
  docker-compose.yml
  README.md
  sql/
    init.sql
  producer-api/
    Dockerfile
    pom.xml
    src/main/java/...
  consumer-worker/
    Dockerfile
    pom.xml
    src/main/java/...

How to Test:
docker compose up --build

Send events:
curl POST ...

Query analytics:
curl GET ...

Stop platform:
docker compose down


Author:
Ghiriish Sridharan
Computer Science and Data Science — Rutgers University

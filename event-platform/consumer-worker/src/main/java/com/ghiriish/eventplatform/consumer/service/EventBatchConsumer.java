package com.ghiriish.eventplatform.consumer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class EventBatchConsumer {

  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;

  public EventBatchConsumer(ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @KafkaListener(
      topics = "${app.kafka.topic:events.v1}",
      containerFactory = "kafkaListenerContainerFactory"
  )
  public void consume(List<String> messages) {
    if (messages == null || messages.isEmpty()) return;

    List<RawEvent> events = new ArrayList<>(messages.size());
    for (String msg : messages) {
      try {
        Map<String, Object> m = objectMapper.readValue(msg, new TypeReference<>() {});
        String eventType = Objects.toString(m.get("eventType"), "");
        String userId = Objects.toString(m.get("userId"), "");
        String sessionId = m.get("sessionId") == null ? null : Objects.toString(m.get("sessionId"));
        Instant ts = Instant.parse(Objects.toString(m.get("timestamp")));
        Object metadataObj = m.get("metadata");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = metadataObj instanceof Map ? (Map<String, Object>) metadataObj : Map.of();
        events.add(new RawEvent(eventType, userId, sessionId, ts, metadata));
      } catch (Exception ignored) {
        // In a real system, you'd DLQ bad messages. Here we skip invalid payloads.
      }
    }

    if (events.isEmpty()) return;

    batchInsertRaw(events);
    upsertDailyAgg(events);
  }

  private void batchInsertRaw(List<RawEvent> events) {
    String sql = """
        INSERT INTO events_raw(event_type, user_id, session_id, ts, metadata)
        VALUES (?, ?, ?, ?, ?::jsonb)
        """;

    jdbcTemplate.batchUpdate(sql, events, 200, (PreparedStatement ps, RawEvent e) -> {
      ps.setString(1, e.eventType());
      ps.setString(2, e.userId());
      ps.setString(3, e.sessionId());
      ps.setTimestamp(4, java.sql.Timestamp.from(e.timestamp()));

      try {
        ps.setString(5, objectMapper.writeValueAsString(
            e.metadata() == null ? Map.of() : e.metadata()
        ));
      } catch (Exception ex) {
        ps.setString(5, "{}");
      }
    });
  }

  private void upsertDailyAgg(List<RawEvent> events) {
    Map<DayTypeKey, Long> counts = new HashMap<>();
    for (RawEvent e : events) {
      LocalDate day = e.timestamp().atZone(ZoneOffset.UTC).toLocalDate();
      DayTypeKey key = new DayTypeKey(day, e.eventType());
      counts.put(key, counts.getOrDefault(key, 0L) + 1L);
    }

    String sql = """
        INSERT INTO event_agg_daily(day, event_type, event_count)
        VALUES (?, ?, ?)
        ON CONFLICT (day, event_type)
        DO UPDATE SET event_count = event_agg_daily.event_count + EXCLUDED.event_count
        """;

    List<Map.Entry<DayTypeKey, Long>> rows = new ArrayList<>(counts.entrySet());
    jdbcTemplate.batchUpdate(sql, rows, 200, (PreparedStatement ps, Map.Entry<DayTypeKey, Long> row) -> {
      ps.setObject(1, row.getKey().day());
      ps.setString(2, row.getKey().eventType());
      ps.setLong(3, row.getValue());
    });
  }

  private record RawEvent(String eventType, String userId, String sessionId, Instant timestamp, Map<String, Object> metadata) {}
  private record DayTypeKey(LocalDate day, String eventType) {}
}
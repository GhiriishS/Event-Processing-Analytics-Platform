package com.ghiriish.eventplatform.producer.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/events")
public class EventIngestController {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final String topic;

  public EventIngestController(
      KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper objectMapper,
      @Value("${app.kafka.topic:events.v1}") String topic
  ) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.topic = topic;
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> publish(@Valid @RequestBody EventRequest req) {
    try {
      Map<String, Object> event = new HashMap<>();
      event.put("eventType", req.eventType());
      event.put("userId", req.userId());
      event.put("sessionId", req.sessionId());
      event.put("timestamp", req.timestamp().toString());
      event.put("metadata", req.metadata() == null ? Map.of() : req.metadata());

      String payload = objectMapper.writeValueAsString(event);
      kafkaTemplate.send(topic, req.userId(), payload);

      return ResponseEntity.status(HttpStatus.ACCEPTED)
          .body(Map.of("status", "queued", "topic", topic));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("status", "error", "message", e.getMessage()));
    }
  }
}

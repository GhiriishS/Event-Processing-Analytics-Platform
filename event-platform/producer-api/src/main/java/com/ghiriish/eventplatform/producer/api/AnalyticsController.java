package com.ghiriish.eventplatform.producer.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

  private final JdbcTemplate jdbcTemplate;

  public AnalyticsController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public record DailyCount(String day, String eventType, long eventCount) {}

  @GetMapping("/events/daily")
  public List<DailyCount> daily(
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to
  ) {
    String sql = """
        SELECT day, event_type, event_count
        FROM event_agg_daily
        WHERE day BETWEEN ? AND ?
        ORDER BY day ASC, event_type ASC
        """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> new DailyCount(
            rs.getDate("day").toString(),
            rs.getString("event_type"),
            rs.getLong("event_count")
        ),
        Date.valueOf(from),
        Date.valueOf(to)
    );
  }
}

package dev.prasadgaikwad.openclaw4j.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Periodically updates heart-beat state and performs background housekeeping.
 */
@Component
public class HeartbeatMonitor {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatMonitor.class);
    private static final Path STATE_FILE = Path.of(".memory/heartbeat-state.json");
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public HeartbeatMonitor(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            Files.createDirectories(STATE_FILE.getParent());
        } catch (IOException e) {
            log.error("Failed to create memory directory for heartbeat state", e);
        }
    }

    /**
     * Runs every 15 minutes to update the last check timestamp and perform
     * housekeeping.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // 15 minutes
    @SuppressWarnings("unchecked")
    public void updateHeartbeat() {
        log.debug("Updating heartbeat state and performing housekeeping...");
        try {
            Map<String, Object> state = loadState();
            Map<String, Object> heartbeat = (Map<String, Object>) state.getOrDefault("heartbeat", new HashMap<>());

            Instant now = Instant.now();
            heartbeat.put("lastCheck", now);
            heartbeat.put("intervalMinutes", 15);

            // Simulation of background check statuses
            List<Map<String, Object>> checks = List.of(
                    Map.of("name", "pending_reminders", "lastRun", now, "status", "OK"),
                    Map.of("name", "memory_compaction", "lastRun", now.minusSeconds(3600), "status", "IDLE"),
                    Map.of("name", "rag_reindex", "lastRun", now.minusSeconds(1800), "status", "OK"));
            heartbeat.put("checks", checks);
            state.put("heartbeat", heartbeat);

            saveState(state);

            // Publish event for other components to react to heartbeat
            eventPublisher.publishEvent(new HeartbeatEvent(now, heartbeat));

            log.info("Heartbeat updated and event published at {}", now);
        } catch (Exception e) {
            log.error("Failed to update heartbeat state", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadState() {
        if (!Files.exists(STATE_FILE)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(STATE_FILE.toFile(), Map.class);
        } catch (IOException e) {
            log.warn("Could not read heartbeat state, starting fresh", e);
            return new HashMap<>();
        }
    }

    private void saveState(Map<String, Object> state) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(STATE_FILE.toFile(), state);
        } catch (IOException e) {
            log.error("Failed to save heartbeat state", e);
        }
    }
}

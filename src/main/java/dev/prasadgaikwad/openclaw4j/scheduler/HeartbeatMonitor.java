package dev.prasadgaikwad.openclaw4j.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Periodically updates heart-beat state and performs background housekeeping.
 */
@Component
public class HeartbeatMonitor {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatMonitor.class);
    private static final Path STATE_FILE = Path.of(".memory/heartbeat-state.json");
    private final ObjectMapper objectMapper;

    public HeartbeatMonitor() {
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
     * Runs every 15 minutes to update the last check timestamp.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // 15 minutes
    @SuppressWarnings("unchecked")
    public void updateHeartbeat() {
        log.debug("Updating heartbeat state...");
        try {
            Map<String, Object> state = loadState();
            Map<String, Object> heartbeat = (Map<String, Object>) state.getOrDefault("heartbeat", new HashMap<>());

            heartbeat.put("lastCheck", Instant.now());
            state.put("heartbeat", heartbeat);

            saveState(state);
            log.info("Heartbeat updated at {}", heartbeat.get("lastCheck"));
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

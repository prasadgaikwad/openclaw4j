package dev.prasadgaikwad.openclaw4j.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;

/**
 * File-backed implementation of the {@link MemoryService}.
 * 
 * <p>
 * This implementation stores memories in the {@code memory/} directory.
 * - Curated memories in {@code MEMORY.md}.
 * - Daily logs in {@code memory/daily/YYYY-MM-DD.md}.
 * </p>
 *
 * @author Prasad Gaikwad
 */
@Service
public class FileMemoryService implements MemoryService {

    private static final Logger log = LoggerFactory.getLogger(FileMemoryService.class);
    private static final Path ROOT_PATH = Path.of(".memory");
    private static final Path MEMORY_MD = ROOT_PATH.resolve("MEMORY.md");
    private static final Path DAILY_LOG_DIR = ROOT_PATH.resolve("daily");

    public FileMemoryService() {
        try {
            Files.createDirectories(DAILY_LOG_DIR);
            if (!Files.exists(MEMORY_MD)) {
                Files.writeString(MEMORY_MD, "# OpenClaw4J Memory\n\nCurated decisions and preferences.\n");
            }
        } catch (IOException e) {
            log.error("Failed to initialize memory directory", e);
        }
    }

    @Override
    public List<String> getRelevantMemories() {
        try {
            if (Files.exists(MEMORY_MD)) {
                return Files.readAllLines(MEMORY_MD).stream()
                        .filter(line -> !line.startsWith("#") && !line.isBlank())
                        .toList();
            }
        } catch (IOException e) {
            log.error("Failed to read MEMORY.md", e);
        }
        return List.of();
    }

    @Override
    public void remember(String fact) {
        try {
            String entry = "- " + fact + "\n";
            Files.writeString(MEMORY_MD, entry, StandardOpenOption.APPEND);
            log.info("Remembered new fact: {}", fact);
        } catch (IOException e) {
            log.error("Failed to write to MEMORY.md", e);
        }
    }

    @Override
    public void logEvent(String event) {
        try {
            Path dailyFile = DAILY_LOG_DIR.resolve(LocalDate.now().toString() + ".md");
            if (!Files.exists(dailyFile)) {
                Files.writeString(dailyFile, "# Daily Log: " + LocalDate.now() + "\n\n");
            }
            String entry = String.format("[%s] %s\n", java.time.LocalTime.now(), event);
            Files.writeString(dailyFile, entry, StandardOpenOption.APPEND);
            log.debug("Logged event to daily log: {}", event);
        } catch (IOException e) {
            log.error("Failed to write to daily log", e);
        }
    }
}

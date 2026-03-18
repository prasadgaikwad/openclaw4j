package dev.prasadgaikwad.openclaw4j.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final Path ROOT_PATH;
    private final Path MEMORY_MD;
    private final Path DAILY_LOG_DIR;

    public FileMemoryService() {
        this(Path.of(".memory"));
    }

    // visible for testing
    FileMemoryService(Path rootPath) {
        this.ROOT_PATH = rootPath;
        this.MEMORY_MD = ROOT_PATH.resolve("MEMORY.md");
        this.DAILY_LOG_DIR = ROOT_PATH.resolve("daily");

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

    @Override
    public List<String> searchMemory(String query) {
        try {
            if (Files.exists(MEMORY_MD)) {
                String lowerQuery = query.toLowerCase();
                return Files.readAllLines(MEMORY_MD).stream()
                        .filter(line -> !line.startsWith("#") && !line.isBlank())
                        .filter(line -> line.toLowerCase().contains(lowerQuery))
                        .toList();
            }
        } catch (IOException e) {
            log.error("Failed to search MEMORY.md", e);
        }
        return List.of();
    }

    @Override
    public List<String> listHistoryDates() {
        List<String> dates = new ArrayList<>();
        try {
            if (Files.exists(DAILY_LOG_DIR)) {
                Files.list(DAILY_LOG_DIR)
                        .filter(p -> p.toString().endsWith(".md"))
                        .map(p -> p.getFileName().toString().replace(".md", ""))
                        .sorted()
                        .forEach(dates::add);
            }
        } catch (IOException e) {
            log.error("Failed to list history dates", e);
        }
        return dates;
    }

    @Override
    public Optional<String> getHistoryLog(String date) {
        Path dailyFile = DAILY_LOG_DIR.resolve(date + ".md");
        try {
            if (Files.exists(dailyFile)) {
                return Optional.of(Files.readString(dailyFile));
            }
        } catch (IOException e) {
            log.error("Failed to read history log for date: {}", date, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean forgetFact(String fact) {
        try {
            if (!Files.exists(MEMORY_MD)) {
                return false;
            }
            String lowerFact = fact.toLowerCase();
            List<String> allLines = Files.readAllLines(MEMORY_MD);
            List<String> retained = allLines.stream()
                    .filter(line -> !line.toLowerCase().contains(lowerFact))
                    .toList();
            boolean removed = retained.size() < allLines.size();
            if (removed) {
                Files.writeString(MEMORY_MD, String.join("\n", retained) + "\n");
                log.info("Forgot fact matching: {}", fact);
            }
            return removed;
        } catch (IOException e) {
            log.error("Failed to forget fact from MEMORY.md", e);
            return false;
        }
    }

    @Override
    public boolean updateFact(String oldFact, String newFact) {
        try {
            if (!Files.exists(MEMORY_MD)) {
                return false;
            }
            String lowerOld = oldFact.toLowerCase();
            List<String> allLines = Files.readAllLines(MEMORY_MD);
            boolean[] updated = {false};
            List<String> newLines = allLines.stream()
                    .map(line -> {
                        if (!updated[0] && line.toLowerCase().contains(lowerOld)) {
                            updated[0] = true;
                            // Preserve the leading "- " bullet if present
                            return line.startsWith("- ") ? "- " + newFact : newFact;
                        }
                        return line;
                    })
                    .toList();
            if (updated[0]) {
                Files.writeString(MEMORY_MD, String.join("\n", newLines) + "\n");
                log.info("Updated fact '{}' to '{}'", oldFact, newFact);
            }
            return updated[0];
        } catch (IOException e) {
            log.error("Failed to update fact in MEMORY.md", e);
            return false;
        }
    }
}

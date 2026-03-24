package dev.prasadgaikwad.openclaw4j.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import dev.prasadgaikwad.openclaw4j.rag.RAGService;

class FileMemoryServiceTest {

    @TempDir
    Path tempDir;

    private FileMemoryService memoryService;
    private Path memoryMd;
    private Path dailyDir;
    private RAGService ragService;

    @BeforeEach
    void setUp() throws IOException {
        memoryMd = tempDir.resolve("MEMORY.md");
        dailyDir = tempDir.resolve("daily");
        // We do not need to pre-create them now because FileMemoryService constructor will manage it.
        // But let's create it manually to ensure known contents before the test logic.
        Files.createDirectories(dailyDir);
        Files.writeString(memoryMd, "# OpenClaw4J Memory\n\nCurated decisions and preferences.\n");
        
        ragService = mock(RAGService.class);
        memoryService = new FileMemoryService(tempDir, ragService);
    }

    @Test
    void searchMemory_shouldReturnMatchingLines() throws IOException {
        Files.writeString(memoryMd, "- User likes dark mode\n- User prefers Java\n",
                java.nio.file.StandardOpenOption.APPEND);

        List<String> results = memoryService.searchMemory("java");
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo("- User prefers Java");
    }

    @Test
    void searchMemory_shouldReturnEmptyWhenNoMatch() throws IOException {
        Files.writeString(memoryMd, "- User likes dark mode\n", java.nio.file.StandardOpenOption.APPEND);

        List<String> results = memoryService.searchMemory("python");
        assertThat(results).isEmpty();
    }

    @Test
    void listHistoryDates_shouldReturnDates() throws IOException {
        Files.writeString(dailyDir.resolve("2024-01-01.md"), "test");
        Files.writeString(dailyDir.resolve("2024-01-02.md"), "test");

        List<String> dates = memoryService.listHistoryDates();
        assertThat(dates).containsExactly("2024-01-01", "2024-01-02");
    }

    @Test
    void getHistoryLog_shouldReturnContent() throws IOException {
        Files.writeString(dailyDir.resolve("2024-01-01.md"), "log content");

        Optional<String> log = memoryService.getHistoryLog("2024-01-01");
        assertThat(log).isPresent().contains("log content");
    }

    @Test
    void getHistoryLog_shouldReturnEmptyIfMissing() {
        Optional<String> log = memoryService.getHistoryLog("2024-01-01");
        assertThat(log).isEmpty();
    }

    @Test
    void forgetFact_shouldRemoveLine() throws IOException {
        Files.writeString(memoryMd, "- Fact 1\n- Fact 2\n", java.nio.file.StandardOpenOption.APPEND);

        boolean result = memoryService.forgetFact("Fact 1");
        assertThat(result).isTrue();

        String content = Files.readString(memoryMd);
        assertThat(content).doesNotContain("Fact 1").contains("Fact 2");
    }

    @Test
    void forgetFact_shouldReturnFalseIfMissing() throws IOException {
        Files.writeString(memoryMd, "- Fact 1\n", java.nio.file.StandardOpenOption.APPEND);

        boolean result = memoryService.forgetFact("Fact 2");
        assertThat(result).isFalse();
    }

    @Test
    void updateFact_shouldReplaceLine() throws IOException {
        Files.writeString(memoryMd, "- Fact 1: old\n- Fact 2\n", java.nio.file.StandardOpenOption.APPEND);

        boolean result = memoryService.updateFact("Fact 1", "Fact 1: new");
        assertThat(result).isTrue();

        String content = Files.readString(memoryMd);
        assertThat(content).contains("- Fact 1: new").doesNotContain("Fact 1: old");
    }

    @Test
    void updateFact_shouldReturnFalseIfMissing() throws IOException {
        Files.writeString(memoryMd, "- Fact 1: old\n", java.nio.file.StandardOpenOption.APPEND);

        boolean result = memoryService.updateFact("Fact 2", "Fact 2: new");
        assertThat(result).isFalse();
    }
}

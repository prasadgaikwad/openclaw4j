package dev.prasadgaikwad.openclaw4j.tool.memory;

import dev.prasadgaikwad.openclaw4j.memory.MemoryService;
import dev.prasadgaikwad.openclaw4j.memory.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryToolTest {

    @Mock
    private MemoryService memoryService;

    @Mock
    private ProfileService profileService;

    private MemoryTool memoryTool;

    @BeforeEach
    void setUp() {
        memoryTool = new MemoryTool(memoryService, profileService);
    }

    @Test
    void searchMemory_shouldFormatResults() {
        when(memoryService.searchMemory("java")).thenReturn(List.of("- User likes Java", "- Needs Java 25"));

        String result = memoryTool.searchMemory("java");

        assertThat(result).contains("Found 2 matching memories:");
        assertThat(result).contains("- User likes Java");
        assertThat(result).contains("- Needs Java 25");
    }

    @Test
    void searchMemory_shouldReturnNotFoundMessage() {
        when(memoryService.searchMemory("python")).thenReturn(List.of());

        String result = memoryTool.searchMemory("python");

        assertThat(result).isEqualTo("No matching memories found for: python");
    }

    @Test
    void listHistory_shouldFormatList() {
        when(memoryService.listHistoryDates()).thenReturn(List.of("2024-01-01", "2024-01-02"));

        String result = memoryTool.listHistory();

        assertThat(result).contains("Available history logs for dates:");
        assertThat(result).contains("2024-01-01");
        assertThat(result).contains("2024-01-02");
    }

    @Test
    void listHistory_shouldReturnNotFoundMessage() {
        when(memoryService.listHistoryDates()).thenReturn(List.of());

        String result = memoryTool.listHistory();

        assertThat(result).isEqualTo("No history logs found.");
    }

    @Test
    void readHistoryLog_shouldReturnContent() {
        when(memoryService.getHistoryLog("2024-01-01")).thenReturn(Optional.of("Log content"));

        String result = memoryTool.readHistoryLog("2024-01-01");

        assertThat(result).isEqualTo("Log content");
    }

    @Test
    void readHistoryLog_shouldReturnNotFoundMessage() {
        when(memoryService.getHistoryLog("2024-01-01")).thenReturn(Optional.empty());

        String result = memoryTool.readHistoryLog("2024-01-01");

        assertThat(result).isEqualTo("No history log found for date: 2024-01-01");
    }

    @Test
    void forgetFact_shouldReturnSuccessMessage() {
        when(memoryService.forgetFact("fact")).thenReturn(true);

        String result = memoryTool.forgetFact("fact");

        verify(memoryService).forgetFact("fact");
        assertThat(result).contains("removed that fact");
    }

    @Test
    void forgetFact_shouldReturnFailureMessage() {
        when(memoryService.forgetFact("fact")).thenReturn(false);

        String result = memoryTool.forgetFact("fact");

        verify(memoryService).forgetFact("fact");
        assertThat(result).contains("could not find a fact");
    }

    @Test
    void updateFact_shouldReturnSuccessMessage() {
        when(memoryService.updateFact("old", "new")).thenReturn(true);

        String result = memoryTool.updateFact("old", "new");

        verify(memoryService).updateFact("old", "new");
        assertThat(result).contains("successfully updated that fact");
    }

    @Test
    void updateFact_shouldReturnFailureMessage() {
        when(memoryService.updateFact("old", "new")).thenReturn(false);

        String result = memoryTool.updateFact("old", "new");

        verify(memoryService).updateFact("old", "new");
        assertThat(result).contains("could not find the old fact");
    }
}

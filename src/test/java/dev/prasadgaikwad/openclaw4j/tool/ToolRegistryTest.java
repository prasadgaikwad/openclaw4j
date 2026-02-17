package dev.prasadgaikwad.openclaw4j.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ToolRegistryTest {

    @Test
    @DisplayName("Registry should contain all provided AITools")
    void registry_shouldContainProvidedTools() {
        // Arrange
        AITool tool1 = mock(AITool.class);
        AITool tool2 = mock(AITool.class);
        List<AITool> tools = List.of(tool1, tool2);

        // Act
        ToolRegistry registry = new ToolRegistry(tools);

        // Assert
        assertEquals(2, registry.getTools().size());
        assertTrue(registry.getTools().contains(tool1));
        assertTrue(registry.getTools().contains(tool2));
    }

    @Test
    @DisplayName("Registry should be empty if no tools are provided")
    void registry_shouldBeEmptyIfNoTools() {
        // Arrange
        List<AITool> tools = List.of();

        // Act
        ToolRegistry registry = new ToolRegistry(tools);

        // Assert
        assertTrue(registry.getTools().isEmpty());
    }
}

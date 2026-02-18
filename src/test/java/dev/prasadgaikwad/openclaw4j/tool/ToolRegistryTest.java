package dev.prasadgaikwad.openclaw4j.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolRegistryTest {

    @Nested
    @DisplayName("Local Tools Registration")
    class LocalTools {
        @Test
        @DisplayName("Registry should contain all provided AITools")
        void registry_shouldContainProvidedTools() {
            // Arrange
            AITool tool1 = mock(AITool.class);
            AITool tool2 = mock(AITool.class);
            List<AITool> tools = List.of(tool1, tool2);
            List<ToolCallbackProvider> mcpProviders = List.of();

            // Act
            ToolRegistry registry = new ToolRegistry(tools, mcpProviders);

            // Assert
            assertEquals(2, registry.getLocalTools().size());
            assertTrue(registry.getLocalTools().contains(tool1));
            assertTrue(registry.getLocalTools().contains(tool2));
        }

        @Test
        @DisplayName("Registry should be empty if no tools are provided")
        void registry_shouldBeEmptyIfNoTools() {
            // Arrange
            List<AITool> tools = List.of();
            List<ToolCallbackProvider> mcpProviders = List.of();

            // Act
            ToolRegistry registry = new ToolRegistry(tools, mcpProviders);

            // Assert
            assertTrue(registry.getLocalTools().isEmpty());
        }
    }

    @Nested
    @DisplayName("MCP Tools Registration")
    class McpTools {
        @Test
        @DisplayName("Registry should collect tools from SyncMcpToolCallbackProvider")
        void shouldCollectMcpTools() {
            // Arrange
            SyncMcpToolCallbackProvider mcpProvider = mock(SyncMcpToolCallbackProvider.class);
            ToolCallback callback = mock(ToolCallback.class);
            when(mcpProvider.getToolCallbacks()).thenReturn(new ToolCallback[] { callback });

            List<AITool> tools = List.of();
            List<ToolCallbackProvider> mcpProviders = List.of(mcpProvider);

            // Act
            ToolRegistry registry = new ToolRegistry(tools, mcpProviders);

            // Assert
            assertEquals(1, registry.getMcpTools().size());
            assertTrue(registry.getMcpTools().contains(callback));
        }

        @Test
        @DisplayName("Registry should ignore non-SyncMcp providers to avoid duplication")
        void shouldIgnoreNonMcpProviders() {
            // Arrange
            ToolCallbackProvider genericProvider = mock(ToolCallbackProvider.class);
            ToolCallback callback = mock(ToolCallback.class);
            // Even if it has callbacks, it's not a SyncMcpToolCallbackProvider
            when(genericProvider.getToolCallbacks()).thenReturn(new ToolCallback[] { callback });

            List<AITool> tools = List.of();
            List<ToolCallbackProvider> mcpProviders = List.of(genericProvider);

            // Act
            ToolRegistry registry = new ToolRegistry(tools, mcpProviders);

            // Assert
            assertTrue(registry.getMcpTools().isEmpty());
        }
    }
}

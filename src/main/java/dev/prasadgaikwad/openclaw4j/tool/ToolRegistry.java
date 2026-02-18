package dev.prasadgaikwad.openclaw4j.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A central registry for managing and discovering both local and remote (MCP)
 * tools.
 *
 * <p>
 * This service aggregates tools from two primary sources:
 * </p>
 * <ol>
 * <li><b>Local Tools</b>: Spring beans that implement the {@link AITool} marker
 * interface.
 * These are typically annotated with {@code @Tool}.</li>
 * <li><b>MCP Tools</b>: Tools dynamically discovered from remote <b>Model
 * Context Protocol</b>
 * servers (e.g., GitHub, Brave Search) at runtime.</li>
 * </ol>
 *
 * <p>
 * The merged list of tools is then provided to the {@link AgentPlanner} for
 * inclusion
 * in the LLM's prompt context.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * 
 * <pre>
 * // Retrieving registered tools for a planner
 * List&lt;Object&gt; locals = toolRegistry.getLocalTools();
 * List&lt;ToolCallback&gt; remote = toolRegistry.getMcpTools();
 * </pre>
 *
 * @author Prasad Gaikwad
 * @see AITool
 * @see SyncMcpToolCallbackProvider
 */
@Service
public class ToolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    private final List<Object> localTools = new ArrayList<>();
    private final List<ToolCallback> mcpTools = new ArrayList<>();

    public ToolRegistry(List<AITool> aiTools, List<ToolCallbackProvider> mcpToolProviders) {
        // Add local tools
        this.localTools.addAll(aiTools);
        logger.info("Registered {} local AITool beans.", aiTools.size());

        // Add MCP tools
        for (ToolCallbackProvider provider : mcpToolProviders) {
            // Only collect from MCP providers to avoid duplication with local aiTools
            if (provider instanceof SyncMcpToolCallbackProvider) {
                Collection<ToolCallback> mcpToolsCollection = Arrays.asList(provider.getToolCallbacks());
                this.mcpTools.addAll(mcpToolsCollection);
                logger.info("Registered {} tools from MCP provider: {}",
                        mcpToolsCollection.size(),
                        provider.getClass().getSimpleName());
            }
        }

        logger.info("Total tools available: {}, MCP tools available: {}", localTools.size(), mcpTools.size());
    }

    /**
     * Returns all discovered tool objects (both beans and ToolMapping/Callbacks).
     */
    public List<Object> getLocalTools() {
        return localTools;
    }

    /**
     * Returns all MCP tools.
     */
    public List<ToolCallback> getMcpTools() {
        return mcpTools;
    }
}

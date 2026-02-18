package dev.prasadgaikwad.openclaw4j.tool;

/**
 * Marker interface for all local AI tools in the OpenClaw4J ecosystem.
 *
 * <p>
 * Any Spring-managed bean that implements this interface is automatically
 * discovered
 * by the {@link ToolRegistry}. Once registered, its methods (specifically those
 * annotated with {@code @Tool}) are exposed to the LLM for invocation.
 * </p>
 *
 * <h3>Example Implementation:</h3>
 * 
 * <pre>
 * &#64;Component
 * public class MyCustomTool implements AITool {
 *     &#64;Tool(description = "Does something cool")
 *     public String doWork(String input) { ... }
 * }
 * </pre>
 *
 * @author Prasad Gaikwad
 * @see ToolRegistry
 */
public interface AITool {
}

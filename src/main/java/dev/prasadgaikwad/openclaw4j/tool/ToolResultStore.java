package dev.prasadgaikwad.openclaw4j.tool;

/**
 * A thread-local store that captures the most recent tool execution result
 * within a request thread.
 *
 * <p>
 * This allows the {@link dev.prasadgaikwad.openclaw4j.agent.AgentPlanner} to
 * access tool outputs after the ReAct loop completes, enabling a context-aware
 * recovery prompt when the primary LLM response is empty.
 * </p>
 *
 * <p>
 * The store must be explicitly cleared at the end of each request (in
 * {@code AgentService}) to prevent thread-local leaks in thread-pool
 * environments.
 * </p>
 *
 * @author Prasad Gaikwad
 */
public class ToolResultStore {

    private static final ThreadLocal<String> store = new ThreadLocal<>();

    private ToolResultStore() {
    }

    /**
     * Stores the given tool result for the current thread.
     *
     * @param result the raw tool output to store
     */
    public static void set(String result) {
        store.set(result);
    }

    /**
     * Retrieves the tool result registered for the current thread.
     *
     * @return the stored result, or {@code null} if none has been set
     */
    public static String get() {
        return store.get();
    }

    /**
     * Clears the tool result for the current thread.
     * Must be called after each request to avoid leaking state across threads.
     */
    public static void clear() {
        store.remove();
    }
}

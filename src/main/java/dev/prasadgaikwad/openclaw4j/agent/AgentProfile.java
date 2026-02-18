package dev.prasadgaikwad.openclaw4j.agent;

import java.util.Map;

/**
 * Profile definition for the agent, capturing personality, system prompts, and
 * user context.
 *
 * <p>
 * This record defines the "identity" of the agent. It includes how the agent
 * describes itself
 * (personality), the explicit instructions provided to the LLM (system prompt),
 * and
 * metadata about the current user.
 * </p>
 *
 * <h3>Example Construction:</h3>
 * 
 * <pre>
 * AgentProfile profile = new AgentProfile(
 *                 "Alice",
 *                 "A concise and professional technical assistant",
 *                 "You are a coding expert named Claw...",
 *                 Map.of("role", "developer"));
 * </pre>
 *
 * @param userName         The name of the user interacting with the agent
 * @param agentPersonality The personality description of the agent
 * @param systemPrompt     The base system prompt for the agent
 * @param preferences      Additional persistent user or system preferences
 *
 * @author Prasad Gaikwad
 */
public record AgentProfile(
                String userName,
                String agentPersonality,
                String systemPrompt,
                Map<String, String> preferences) {
}

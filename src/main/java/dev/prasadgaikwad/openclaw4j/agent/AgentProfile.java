package dev.prasadgaikwad.openclaw4j.agent;

import java.util.Map;

/**
 * Profile definition for the agent, including personality and system prompt.
 * 
 * @param userName         The name of the user interacting with the agent
 * @param agentPersonality The personality description of the agent
 * @param systemPrompt     The base system prompt for the agent
 * @param preferences      Additional preferences
 */
public record AgentProfile(
        String userName,
        String agentPersonality,
        String systemPrompt,
        Map<String, String> preferences) {
}

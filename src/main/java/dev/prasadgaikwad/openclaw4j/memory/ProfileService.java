package dev.prasadgaikwad.openclaw4j.memory;

import dev.prasadgaikwad.openclaw4j.agent.AgentProfile;

/**
 * Service for loading and managing agent identity and user preferences.
 *
 * <p>
 * This service reads from profile files (USER.md, SOUL.md, TOOLS.md) to
 * construct a rich {@link AgentProfile} that informs the agent's behavior.
 * </p>
 *
 * @author Prasad Gaikwad
 */
public interface ProfileService {

    /**
     * Loads the full agent profile from the underlying filesystem.
     *
     * @return the loaded AgentProfile
     */
    AgentProfile getProfile();

    /**
     * Updates a specific preference in the user profile (USER.md).
     *
     * @param key   the preference key
     * @param value the preference value
     */
    void updatePreference(String key, String value);
}

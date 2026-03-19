# Profile and Identity Reference

The profile defines who OpenClaw4J is and how it should behave. It manages the user's personality, core instructions, and specific preferences.

## Features

-   **User Identity**: Stores the user's name and preferred tone.
-   **Soul Definition**: Defines the agent's personality and soul for behavioral consistency.
-   **Environment Base**: Stays aware of its operating context (e.g., Development, Server URL).

## Available Tools

#### `updateUserPreference(String key, String value)`
Updates a specific user preference in the profile (`USER.md`). The key can be any string (e.g., `tone`, `notification_settings`).

#### `updateSoul(String soulContent)`
Updates the agent's core personality and soul definition in `SOUL.md`. This allows the agent's behavior and responses to evolve as the relationship with the user develops.

#### `updateEnvironmentFact(String fact)`
Adds an environmental fact to `TOOLS.md` (e.g., repository names, server URLs). These are used to ground the agent's knowledge in its current environment.

---

## Configuration Files

The profile is backed by files in the `.memory/profiles/` directory:

-   **`USER.md`**: Contains the user's name and preference list.
-   **`SOUL.md`**: Defines the agent's behavioral persona.
-   **`TOOLS.md`**: Stores environmental facts and tool-specific constants.

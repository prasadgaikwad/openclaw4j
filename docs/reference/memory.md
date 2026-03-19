# Memory Reference

OpenClaw4J features a multi-layered memory system that allows the agent to retain information across sessions and track historical events.

## Long-Term Memory (MEMORY.md)

Long-term memory is used to store curated facts, user preferences, and important decisions that should persist indefinitely.

### Features

-   **Persistence**: Facts are saved to `.memory/MEMORY.md`.
-   **Structure**: Memories are stored as a bulleted list of strings.
-   **Curation**: The agent can explicitly decide what to remember, search for, or delete.

### Available Tools

#### `remember(String fact)`
Saves an important fact or user preference into long-term memory for future recall. Use this when the user explicitly asks you to remember something or when you identify a piece of information that will be useful in future conversations.

#### `searchMemory(String query)`
Searches the `MEMORY.md` file for lines matching a keyword or phrase (case-insensitive). This is useful when you need to recall specific information that might not be in the immediate context.

#### `forgetFact(String fact)`
Removes a specific fact from long-term memory. Use this to clean up obsolete or incorrect information. It matches lines containing the provided string.

#### `updateFact(String oldFact, String newFact)`
Replaces an existing fact in long-term memory with a updated version. This is the preferred way to correct or evolve stored information without losing context.

---

## Daily History Logs

Daily logs capture raw events, scratch notes, and a chronological record of interactions.

### Features

-   **Daily Rotation**: Logs are stored in `.memory/daily/YYYY-MM-DD.md`.
-   **Raw Record**: Useful for debugging, auditing, or retrieving snippets of past conversations that aren't "curated" into long-term memory.

### Available Tools

#### `logEvent(String event)`
Logs a raw event or scratch note to the current day's log file. These are timestamped automatically.

#### `listHistory()`
Lists all dates for which a daily history log exists. Returns a list in `YYYY-MM-DD` format.

#### `readHistoryLog(String date)`
Retrieves the full content of the daily history log for a specific date.

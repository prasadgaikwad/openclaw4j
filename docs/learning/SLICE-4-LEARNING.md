# Slice 4 Learning: Layered Memory System

In Slice 4, we implemented a persistent, layered memory system for OpenClaw4J. This allows the agent to maintain context across sessions and refine its identity over time.

## Conceptual Model: Memory Layers

| Layer | Implementation | Purpose | Retention |
|-------|----------------|---------|-----------|
| **Short-Term** | `ShortTermMemory` (In-memory) | Conversation context (last N messages) | Session |
| **Long-Term Curated** | `MEMORY.md` (Markdown) | Key facts, user preferences, past decisions | Permanent |
| **Long-Term Raw** | `.memory/daily/*.md` | Raw event log of all agent interactions | Permanent |
| **Identity/Profile** | `.memory/profiles/*.md`| Agent soul, user persona, environment facts | Permanent |

## Key Technical Decisions

### 1. File-Backed Persistence
We chose Markdown files (`.md`) for long-term memory.
- **Why?** They are human-readable, easily editable by the user, and simple to parse/append to programmatically.
- **Security:** The `.memory/` directory is hidden and excluded from `.git` to prevent leaking private context.

### 2. Profile vs. Memory
Split the context into "Who am I?" (Profile) and "What happened before?" (Memory).
- **Profile:** Loaded once at start. Defines the system prompt and persona.
- **Memory:** Loaded per-request. Supplies relevant facts to the LLM to provide continuity.

### 3. Agent Self-Curation & Identity
The `MemoryTool` allows the agent to decide what is "worth remembering" and even how it should behave. The agent now has direct write access to its own profile files through the `ProfileService`.
- **`remember(fact)`**: Curates important information into `MEMORY.md`.
- **`updateUserPreference(key, value)`**: Updates preferences in `USER.md`.
- **`updateSoul(soulContent)`**: Programmatically changes the agent's personality instructions in `SOUL.md`.
- **`updateEnvironmentFact(fact)`**: Adds context about tools or environments to `TOOLS.md`.
- **`logEvent(event)`**: Records context for debugging or history.

## Integration with Agent Loop

The `AgentService` now follows this pipeline:
1. Receive Inbound Message.
2. Load Agent Profile (Soul, User name).
3. Load Relevant Memories (Line-by-line from `MEMORY.md`).
4. Assemble `AgentContext`.
5. `AgentPlanner` appends memory fragments to the System Message.
6. Execution loop proceeds with higher-fidelity context.

## Troubleshooting & Future Improvements
- **Context Window Blowup:** As `MEMORY.md` and profile files grow, we will need the Semantic Retrieval (RAG) planned for Slice 5.
- **Concurrency:** Simple file appending is fine for single-user localized use cases, but might need locks for multi-user environments.

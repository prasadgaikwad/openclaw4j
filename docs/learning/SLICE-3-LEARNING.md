# Slice 3 — Learning Guide: Tools (MCP)

> **What you built:** An agent that doesn't just talk, but *acts*. It can now interact with external tools discovered from MCP servers (like GitHub).
> **Concepts covered:** Model Context Protocol (MCP), `ToolCallbackProvider`, `ToolCallAdvisor`, and the automatic tool execution loop.

---

## 1. Model Context Protocol (MCP) & Spring AI

### 1.1 What are Tools?
In LLM terms, **Tools** (or Functions) are external capabilities that the model can choose to invoke. The model doesn't "run" the code itself; instead, it outputs a special "tool call" instruction, which your application executes, returning the result to the model.

### 1.2 MCP Servers
Instead of writing every tool manually, we use **MCP (Model Context Protocol)** to connect to external "tool servers". For example, a GitHub MCP server provides tools for reading issues, creating PRs, etc., without requiring us to write the boilerplate.

**Where in the code:** `application.yml` (MCP client config), `ToolRegistry`

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          servers:
            github:
              command: npx
              args: ["-y", "@modelcontextprotocol/server-github"]
              env:
                GITHUB_PERSONAL_ACCESS_TOKEN: ${GITHUB_TOKEN}
```

---

## 2. Tool Registry & Discovery

### 2.1 The ToolRegistry
The `ToolRegistry` now handles two types of tools:
1.  **Local Tools**: Spring beans implementing the `AITool` marker interface.
2.  **MCP Tools**: Tools dynamically discovered from MCP servers, provided as `ToolCallback` objects.

**Where in the code:** `ToolRegistry`

```java
// Inside ToolRegistry constructor
for (ToolCallbackProvider provider : mcpToolProviders) {
    if (provider instanceof SyncMcpToolCallbackProvider) {
        this.mcpTools.addAll(Arrays.asList(provider.getToolCallbacks()));
    }
}
```

---

## 3. Tool Calling Workflow

### 3.1 The Automatic ReAct Loop
When tools are enabled, the interaction changes from a single request/response to a loop. Spring AI 1.1.2 makes this automatic using the **`ToolCallAdvisor`**.

**Where in the code:** `AgentPlanner`

```java
chatClient.prompt()
    .advisors(ToolCallAdvisor.builder().build()) // Enables the Thinking/Acting loop
    .messages(messages)
    .tools(context.localTools().toArray())
    .toolCallbacks(context.mcpTools())
    .call()
    .content();
```

Without the advisor, the LLM might return a "tool call" but the application wouldn't know to execute it and send the answer back. The advisor automates this `Think -> Act -> Observe -> Think` cycle.

---

## 4. Troubleshooting

### 4.1 Empty Response Error
If an LLM decides to call a tool but doesn't provide any natural language text in its first response, the application might crash with `IllegalArgumentException: content must not be null or blank`.

**Solution:** 
1.  Ensure `ToolCallAdvisor` is active so the loop completes.
2.  Add a fallback in `AgentService` to handle cases where the LLM still returns an empty string after processing.

### 4.2 Dependency Management
Ensure the `spring-ai-starter-mcp-client` is in your `build.gradle` to enable MCP capabilities.

---

## 5. Exercises

1. **Add a new MCP Server:** Add the `google-maps` or `brave-search` MCP server to `application.yml` and see if the agent can use it.
2. **Local @Tool:** Create a new class with a method annotated with `@Tool` and see how it differs from MCP-based tools.
3. **Inspect the advisor:** Debug `ToolCallAdvisor` to see how it intercepts the response and triggers the tool execution.

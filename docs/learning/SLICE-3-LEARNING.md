# Slice 3 — Learning Guide: Tools (MCP)

> **What you built:** An agent that doesn't just talk, but *acts*. It can now create GitHub issues and interact with Slack history.
> **Concepts covered:** Model Context Protocol (MCP), Spring AI `@Tool` annotation, and Function Calling (Tooling).

---

## 1. Model Context Protocol (MCP) & Spring AI

### 1.1 What are Tools?
In LLM terms, **Tools** (or Functions) are external capabilities that the model can choose to invoke. The model doesn't "run" the code itself; instead, it outputs a special "tool call" instruction, which your application executes, returning the result to the model.

### 1.2 The `@Tool` Annotation
Spring AI 1.1+ introduces a simplified way to define tools using the `@Tool` annotation on any bean method.

**Where in the code:** `GitHubTool`, `SlackTool`

```java
@Tool(description = "Creates a new issue in a GitHub repository")
public String createGitHubIssue(String title, String body, String repo) {
    // ... implementation ...
}
```

**Key rules:**
- **Description is Mandatory:** The LLM uses the description to decide *when* to use the tool.
- **Parameters are Schematized:** Spring AI automatically generates a JSON Schema from your method parameters (names and types) to tell the LLM how to call it.

---

## 2. Tool Registry & Discovery

### 2.1 The ToolRegistry
To keep the agent modular, we use a `ToolRegistry` to manage which tools are "visible" to the agent at any given time.

**Where in the code:** `ToolRegistry`

```java
public List<String> getAvailableToolNames() {
    return List.of("gitHubTool", "slackTool");
}
```

By returning bean names, we allow the `AgentPlanner` to dynamically attach these capabilities to specific LLM requests.

---

## 3. Tool Calling Workflow

### 3.1 The Reasoning Loop (ReAct)
When tools are enabled, the interaction with the LLM changes from a single request/response to a loop:
1. **User:** "Create a bug report for the login issue."
2. **LLM:** "I'll use `createGitHubIssue`. Parameters: {title: 'Login Issue', ...}"
3. **Application:** Executes `gitHubTool.createGitHubIssue(...)`.
4. **Application:** Sends the *result* back to the LLM.
5. **LLM:** "Issue created! You can view it here: [link]"

### 3.2 ChatClient Integration
Using the fluent API, we enable tools per request using `.toolNames()`.

**Where in the code:** `AgentPlanner`

```java
chatClient.prompt()
    .messages(messages)
    .toolNames(context.availableTools().toArray(new String[0]))
    .call()
    .content();
```

---

## 4. Troubleshooting

### 4.1 "Method undefined" Errors
Spring AI is evolving rapidly. Method names like `.functions()` vs `.toolNames()` vs `.tools()` can vary between version 1.0.0 and 1.1.2. 
- In **1.1.2**, use `.toolNames()` for bean-based reference.

### 4.2 LLM "Hallucination" of Tools
If the LLM tries to call a tool that doesn't exist, it usually means your descriptions are too vague or you haven't registered the bean name correctly in the `ChatClient` spec.

---

## 5. Exercises

1. **Add a Calculator:** Create a `MathTool` with an `@Tool` annotated method for complex calculations and register it.
2. **Restrict Tools:** Modify `AgentService` to only provide the `GitHubTool` if the message comes from a specific "admin" user ID.
3. **Inspect the Loop:** Use a debugger or log point in `AgentPlanner` to see the `ChatResponse` before `.content()` is called. Look for `ToolCalls` in the message metadata.

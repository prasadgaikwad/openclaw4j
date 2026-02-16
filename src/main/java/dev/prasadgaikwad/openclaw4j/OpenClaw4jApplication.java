package dev.prasadgaikwad.openclaw4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the OpenClaw4J autonomous agent.
 *
 * <p>
 * OpenClaw4J is an AI-powered agent that lives inside your messaging channels.
 * It reads, understands, plans, and executes tasks using LLM reasoning,
 * MCP tools, RAG retrieval, and layered persistent memory.
 * </p>
 *
 * <h2>MVP Slice 1 — Foundation</h2>
 * <p>
 * This slice establishes the project scaffold, channel adapter pattern,
 * and a simple echo agent service connected to Slack.
 * </p>
 *
 * @see <a href="docs/PRD.md">Product Requirements Document</a>
 */
@SpringBootApplication
public class OpenClaw4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenClaw4jApplication.class, args);
    }
}

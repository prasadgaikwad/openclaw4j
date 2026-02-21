package dev.prasadgaikwad.openclaw4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the OpenClaw4J autonomous agent.
 *
 * <p>
 * OpenClaw4J is an AI-powered agent designed to live within messaging channels
 * like Slack.
 * It follows the <b>ReAct (Reasoning and Acting)</b> pattern to execute complex
 * tasks
 * by combining LLM reasoning, Model Context Protocol (MCP) tools, RAG
 * retrieval,
 * and a layered persistent memory system.
 * </p>
 *
 * <p>
 * This application is built using Spring Boot 3.5.10 and Spring AI 1.1.2,
 * leveraging
 * Java 25 features such as records, sealed classes, and virtual threads.
 * </p>
 *
 * <h3>Usage Examples:</h3>
 * 
 * <pre>
 * // Run with default settings (OpenAI)
 * ./gradlew bootRun
 *
 * // Run with Ollama profile for local LLM execution
 * ./gradlew bootRun --args='--spring.profiles.active=ollama'
 * </pre>
 *
 * @author Prasad Gaikwad
 * @see <a href="README.md">README.md</a>
 * @see <a href="docs/PRD.md">Product Requirements Document</a>
 */
@EnableScheduling
@SpringBootApplication
public class OpenClaw4JApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenClaw4JApplication.class, args);
    }
}

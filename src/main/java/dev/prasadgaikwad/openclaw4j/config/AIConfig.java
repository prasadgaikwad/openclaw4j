package dev.prasadgaikwad.openclaw4j.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Configuration to handle multiple LLM providers (OpenAI, Ollama, etc.).
 *
 * <p>
 * This configuration class uses {@code @ConditionalOnProperty} to dynamically
 * instantiate
 * the appropriate {@link ChatClient} based on the application's configuration.
 * This allows
 * for seamless switching between cloud-based providers (like OpenAI) and
 * local-hosted ones
 * (like Ollama) depending on the environment or user preference.
 * </p>
 *
 * <h3>Example configuration in application.yml:</h3>
 * 
 * <pre>
 * openclaw4j:
 *   ai:
 *     provider: openai # Switch to 'ollama' for local execution
 * </pre>
 *
 * @author Prasad Gaikwad
 */
@Configuration
public class AIConfig {

    @Bean
    @ConditionalOnProperty(name = "openclaw4j.ai.provider", havingValue = "openai", matchIfMissing = true)
    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @ConditionalOnProperty(name = "openclaw4j.ai.provider", havingValue = "ollama")
    public ChatClient ollamaChatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}

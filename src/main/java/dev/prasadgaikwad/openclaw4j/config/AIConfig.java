package dev.prasadgaikwad.openclaw4j.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Configuration to handle multiple LLM providers.
 * 
 * We use @ConditionalOnProperty to decide which ChatClient bean to create.
 * By specifying the concrete model type (e.g., OpenAiChatModel) in the
 * parameter,
 * we avoid NoUniqueBeanDefinitionException even if multiple starters are
 * active.
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

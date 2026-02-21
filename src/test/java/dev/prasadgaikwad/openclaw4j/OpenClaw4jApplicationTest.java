package dev.prasadgaikwad.openclaw4j;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.vectorstore.VectorStore;
import javax.sql.DataSource;
import dev.prasadgaikwad.openclaw4j.rag.RAGService;

/**
 * Smoke test that verifies the Spring Boot application context loads
 * successfully.
 */
@SpringBootTest
@ActiveProfiles("openai")
@TestPropertySource(properties = {
        "openclaw4j.channel.slack.bot-token=xoxb-test-token",
        "openclaw4j.channel.slack.signing-secret=test-signing-secret",
        "spring.ai.openai.api-key=test"
})
class OpenClaw4jApplicationTest {

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RAGService ragService;

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        // If this test passes, the Spring Boot application context started
        // without errors. All beans were created and wired correctly.
    }
}

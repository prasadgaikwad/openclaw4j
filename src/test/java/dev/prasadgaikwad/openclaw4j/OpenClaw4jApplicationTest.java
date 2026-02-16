package dev.prasadgaikwad.openclaw4j;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test that verifies the Spring Boot application context loads
 * successfully.
 *
 * <h2>What This Tests</h2>
 * <p>
 * This test starts the full Spring Boot application context. If any beans
 * fail to configure, any circular dependencies exist, or any required
 * properties
 * are missing, this test will fail. It's a fast sanity check that everything
 * is wired together correctly.
 * </p>
 *
 * <h2>Spring Boot Concept: @SpringBootTest</h2>
 * <p>
 * {@code @SpringBootTest} starts the full application context, finding
 * the {@code @SpringBootApplication} class automatically. It's the
 * integration-level equivalent of running {@code ./gradlew bootRun}.
 * </p>
 *
 * <h2>Test Properties</h2>
 * <p>
 * We override the Slack properties with dummy values so the test doesn't
 * require real Slack credentials. The Bolt App bean will be created but
 * won't actually connect to Slack.
 * </p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "openclaw4j.channel.slack.bot-token=xoxb-test-token",
        "openclaw4j.channel.slack.signing-secret=test-signing-secret"
})
class OpenClaw4jApplicationTest {

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        // If this test passes, the Spring Boot application context started
        // without errors. All beans were created and wired correctly.
    }
}

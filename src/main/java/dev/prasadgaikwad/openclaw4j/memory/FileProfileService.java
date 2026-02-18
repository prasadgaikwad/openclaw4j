package dev.prasadgaikwad.openclaw4j.memory;

import dev.prasadgaikwad.openclaw4j.agent.AgentProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * File-backed implementation of {@link ProfileService}.
 * 
 * <p>
 * This service reads from {@code memory/profiles/} directory:
 * - {@code USER.md}: Contains user information and preferences.
 * - {@code SOUL.md}: Defines the agent's personality and soul.
 * - {@code TOOLS.md}: Contains environment-specific facts (e.g., repository
 * names).
 * </p>
 *
 * @author Prasad Gaikwad
 */
@Service
public class FileProfileService implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(FileProfileService.class);
    private static final Path PROFILE_DIR = Path.of(".memory", "profiles");
    private static final Path USER_MD = PROFILE_DIR.resolve("USER.md");
    private static final Path SOUL_MD = PROFILE_DIR.resolve("SOUL.md");
    private static final Path TOOLS_MD = PROFILE_DIR.resolve("TOOLS.md");

    public FileProfileService() {
        try {
            Files.createDirectories(PROFILE_DIR);
            ensureFileExists(USER_MD, "User Name: Prasad\n\nPreferences:\n- Tone: Professional yet friendly\n");
            ensureFileExists(SOUL_MD, "Agent Soul: You are OpenClaw4J, a helpful autonomous assistant.\n");
            ensureFileExists(TOOLS_MD, "Environment: Development\n");
        } catch (IOException e) {
            log.error("Failed to initialize profile directory", e);
        }
    }

    private void ensureFileExists(Path path, String defaultContent) throws IOException {
        if (!Files.exists(path)) {
            Files.writeString(path, defaultContent);
        }
    }

    @Override
    public AgentProfile getProfile() {
        String userName = "User";
        String soul = "Helpful Assistant";
        String environment = "";
        Map<String, String> preferences = new HashMap<>();

        try {
            userName = parseFirstValue(USER_MD, "User Name:");
            soul = Files.readString(SOUL_MD).trim();
            environment = Files.readString(TOOLS_MD).trim();

            // Basic preference parsing (lines starting with -)
            Files.readAllLines(USER_MD).stream()
                    .filter(line -> line.startsWith("-"))
                    .forEach(line -> {
                        String[] parts = line.substring(1).split(":", 2);
                        if (parts.length == 2) {
                            preferences.put(parts[0].trim().toLowerCase(), parts[1].trim());
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to load profile files", e);
        }

        String personality = soul + "\n\nEnvironment Context:\n" + environment;
        String systemPrompt = "You are OpenClaw4J. You use your tools and memory to assist " + userName + ".\n"
                + personality;

        return new AgentProfile(userName, personality, systemPrompt, preferences);
    }

    @Override
    public void updatePreference(String key, String value) {
        try {
            String entry = String.format("- %s: %s\n", key, value);
            Files.writeString(USER_MD, entry, java.nio.file.StandardOpenOption.APPEND);
            log.info("Updated user preference: {} = {}", key, value);
        } catch (IOException e) {
            log.error("Failed to update preferences in USER.md", e);
        }
    }

    @Override
    public void updateSoul(String soulContent) {
        try {
            Files.writeString(SOUL_MD, soulContent);
            log.info("Updated agent soul definition");
        } catch (IOException e) {
            log.error("Failed to update SOUL.md", e);
        }
    }

    @Override
    public void updateEnvironmentFact(String fact) {
        try {
            String entry = String.format("- %s\n", fact);
            Files.writeString(TOOLS_MD, entry, java.nio.file.StandardOpenOption.APPEND);
            log.info("Updated environment fact: {}", fact);
        } catch (IOException e) {
            log.error("Failed to update TOOLS.md", e);
        }
    }

    private String parseFirstValue(Path path, String prefix) throws IOException {
        return Files.readAllLines(path).stream()
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()).trim())
                .findFirst()
                .orElse("");
    }
}

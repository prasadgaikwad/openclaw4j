package dev.prasadgaikwad.openclaw4j.config;

import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageChangedEvent;
import com.slack.api.model.event.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * Runtime hints for GraalVM Native Image to support the Slack Bolt SDK.
 *
 * <p>
 * The Slack SDK uses Gson with {@code Unsafe.allocateInstance()} to
 * deserialize API response DTOs. GraalVM native image requires all such
 * classes to be registered with {@code UNSAFE_ALLOCATED}.
 * </p>
 *
 * <p>
 * This registrar scans the Slack SDK's response and model packages at
 * AOT build time and registers all classes for reflection and unsafe
 * allocation, preventing runtime errors like
 * {@code "Type ... is instantiated reflectively but was never registered"}.
 * </p>
 */
@Configuration
@ImportRuntimeHints(SlackRuntimeHints.SlackRegistrar.class)
public class SlackRuntimeHints {

    static class SlackRegistrar implements RuntimeHintsRegistrar {

        private static final Logger log = LoggerFactory.getLogger(SlackRegistrar.class);

        // All MemberCategories needed for Gson deserialization
        private static final MemberCategory[] GSON_MEMBER_CATEGORIES = {
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.UNSAFE_ALLOCATED
        };

        @Override
        public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {

            // ─────────────────────────────────────────────
            // 1. Event model classes — required by EventsApiPayloadParser
            // (uses getDeclaredConstructors() + newInstance() + getType())
            // ─────────────────────────────────────────────
            var eventClasses = new Class<?>[] {
                    MessageEvent.class,
                    MessageChangedEvent.class,
                    AppMentionEvent.class,
            };
            for (Class<?> clazz : eventClasses) {
                hints.reflection().registerType(clazz, builder -> builder.withMembers(
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.DECLARED_FIELDS,
                        MemberCategory.UNSAFE_ALLOCATED));
            }

            // ─────────────────────────────────────────────
            // 2. Slack SDK internal classes
            // ─────────────────────────────────────────────
            hints.reflection().registerType(com.slack.api.bolt.AppConfig.class, builder -> builder.withMembers(
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS));
            hints.reflection().registerType(com.slack.api.bolt.App.class, builder -> builder.withMembers(
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS));
            hints.reflection().registerType(com.slack.api.SlackConfig.class, builder -> builder.withMembers(
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS));

            // ─────────────────────────────────────────────
            // 3. Gson
            // ─────────────────────────────────────────────
            hints.reflection().registerType(com.google.gson.Gson.class, builder -> builder.withMembers(
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS));
            hints.reflection().registerType(com.google.gson.GsonBuilder.class, builder -> builder.withMembers(
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS));

            // ─────────────────────────────────────────────
            // 4. Bulk-register Slack API DTOs (response + model classes)
            // These are deserialized by Gson via Unsafe.allocateInstance()
            // ─────────────────────────────────────────────
            String[] packagesToScan = {
                    "com.slack.api",
                    "org.springframework.ai",
            };

            var resolver = new PathMatchingResourcePatternResolver();
            for (String pkg : packagesToScan) {
                registerPackageClasses(hints, resolver, pkg, classLoader);
            }
        }

        private void registerPackageClasses(RuntimeHints hints,
                PathMatchingResourcePatternResolver resolver,
                String packageName,
                ClassLoader classLoader) {
            String pattern = "classpath*:" + packageName.replace('.', '/') + "/**/*.class";
            try {
                Resource[] resources = resolver.getResources(pattern);
                ClassLoader cl = classLoader != null ? classLoader : getClass().getClassLoader();
                for (Resource resource : resources) {
                    String path = resource.getURL().getPath();
                    // Extract class name from path
                    int idx = path.indexOf(packageName.replace('.', '/'));
                    if (idx >= 0) {
                        String className = path.substring(idx)
                                .replace('/', '.')
                                .replace(".class", "");
                        // Skip inner classes that aren't static (they can't be instantiated standalone)
                        if (className.contains("$") && className.matches(".*\\$\\d+.*")) {
                            continue;
                        }
                        try {
                            Class<?> clazz = Class.forName(className, false, cl);
                            hints.reflection().registerType(clazz,
                                    builder -> builder.withMembers(GSON_MEMBER_CATEGORIES));
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            log.trace("Skipping class {}: {}", className, e.getMessage());
                        }
                    }
                }
                log.info("Registered {} classes from package {} for GraalVM native image",
                        resources.length, packageName);
            } catch (IOException e) {
                log.warn("Failed to scan package {}: {}", packageName, e.getMessage());
            }
        }
    }
}

package io.github.anthonydeseille.llmeval4j.prompts;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Prompts kept in files and versioned by their content.
 * <p>
 * The version of a prompt is a short fingerprint of the exact text sent to the model: it changes by
 * itself on every edit, with no number to bump by hand, and ties an answer, an evaluation report or a
 * line of history to the prompt that produced it. Store it next to every result.
 * <p>
 * Loaded text is normalized (UTF-8, {@code \n} line endings, trailing whitespace removed) so that the
 * version depends neither on the operating system nor on the editor.
 */
public final class Prompts {

    /** Number of hexadecimal characters of the SHA-256 fingerprint kept as the version. */
    public static final int VERSION_LENGTH = 8;

    private Prompts() {
    }

    /**
     * Text of a classpath resource such as {@code /prompts/answer.md}, normalized.
     *
     * @throws IllegalStateException if the resource does not exist: a missing prompt should stop the
     *                               application, not send an empty prompt
     */
    public static String load(String resource) {
        try (InputStream in = Prompts.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Prompt not found: " + resource);
            }
            return normalize(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read prompt " + resource, e);
        }
    }

    /** Line endings as {@code \n}, trailing whitespace removed. */
    public static String normalize(String text) {
        return text.replace("\r\n", "\n").stripTrailing();
    }

    /** Version of a prompt: the first {@value #VERSION_LENGTH} hexadecimal characters of its SHA-256. */
    public static String version(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, VERSION_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

package io.github.anthonydeseille.llmeval4j.prompts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PromptsTest {

    @Test
    void version_is_the_start_of_the_sha256_of_the_text() {
        // echo -n "hello" | shasum -a 256 → 2cf24dba5fb0a30e26e83b2ac5b9e29e...
        assertThat(Prompts.version("hello")).isEqualTo("2cf24dba");
    }

    @Test
    void any_edit_changes_the_version() {
        assertThat(Prompts.version("Answer in French.")).isNotEqualTo(Prompts.version("Answer in French!"));
    }

    @Test
    void version_does_not_depend_on_line_endings_or_trailing_whitespace() {
        assertThat(Prompts.normalize("line 1\r\nline 2\n\n  ")).isEqualTo("line 1\nline 2");
    }

    @Test
    void loaded_prompt_is_normalized() {
        // the file ends with a blank line and uses \r\n
        assertThat(Prompts.load("/prompts/greeting.md")).isEqualTo("You are a helpful assistant.\nBe brief.");
    }

    @Test
    void missing_prompt_is_an_error() {
        assertThatThrownBy(() -> Prompts.load("/prompts/missing.md"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("/prompts/missing.md");
    }
}

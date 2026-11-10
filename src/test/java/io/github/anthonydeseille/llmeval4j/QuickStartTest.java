package io.github.anthonydeseille.llmeval4j;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.anthonydeseille.llmeval4j.pricing.ModelPrice;
import io.github.anthonydeseille.llmeval4j.pricing.PriceTable;
import io.github.anthonydeseille.llmeval4j.prompts.Prompts;
import org.junit.jupiter.api.Test;

/** The example of the README, kept compiling and passing. */
class QuickStartTest {

    @Test
    void quick_start() {
        // A prompt from src/main/resources/prompts/, and its version to store next to each result
        String prompt = Prompts.load("/prompts/greeting.md");
        String promptVersion = Prompts.version(prompt);

        // Default prices, completed with a model of your own
        PriceTable prices = PriceTable.defaults()
                .with("my-fine-tuned-model", new ModelPrice(3.0, 15.0));

        // Token counts as reported by your LLM client after the call
        double dollars = prices.cost("claude-sonnet-5-5", 12_000, 800);

        assertThat(promptVersion).hasSize(8);
        assertThat(dollars).isEqualTo(0.032);
    }
}

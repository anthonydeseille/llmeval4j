package io.github.anthonydeseille.llmeval4j.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class PriceTableTest {

    @Test
    void cost_of_a_call_with_a_default_price() {
        // 10,000 input tokens at $4/M + 1,000 output tokens at $20/M
        assertThat(PriceTable.defaults().cost("claude-opus-5-5", 10_000, 1_000)).isCloseTo(0.06, within(1e-9));
    }

    @Test
    void unknown_model_has_an_unknown_cost_not_a_zero_cost() {
        assertThat(PriceTable.defaults().cost("some-model", 10_000, 1_000)).isNaN();
        assertThat(PriceTable.defaults().find("some-model")).isEmpty();
    }

    @Test
    void with_overrides_a_price_without_changing_the_original_table() {
        PriceTable table = PriceTable.defaults().with("claude-opus-5-5", new ModelPrice(1, 2));

        assertThat(table.find("claude-opus-5-5")).contains(new ModelPrice(1, 2));
        assertThat(PriceTable.defaults().find("claude-opus-5-5")).contains(new ModelPrice(4, 20));
    }

    @Test
    void properties_add_and_override_prices() {
        Properties properties = new Properties();
        properties.setProperty("my-model.input", "1.5");
        properties.setProperty("my-model.output", " 3 ");
        properties.setProperty("claude-haiku-5-5.input", "0.2");
        properties.setProperty("claude-haiku-5-5.output", "1");

        PriceTable table = PriceTable.defaults().withProperties(properties);

        assertThat(table.find("my-model")).contains(new ModelPrice(1.5, 3));
        assertThat(table.find("claude-haiku-5-5")).contains(new ModelPrice(0.2, 1));
        assertThat(table.find("claude-opus-5-5")).contains(new ModelPrice(4, 20));
    }

    @Test
    void model_id_with_dots_is_kept_whole() {
        Properties properties = new Properties();
        properties.setProperty("mistral-7b-v0.3.input", "0");
        properties.setProperty("mistral-7b-v0.3.output", "0");

        assertThat(PriceTable.empty().withProperties(properties).find("mistral-7b-v0.3")).contains(ModelPrice.FREE);
    }

    @Test
    void a_price_with_only_one_of_its_two_values_is_refused() {
        Properties onlyOutput = new Properties();
        onlyOutput.setProperty("my-model.output", "3");
        Properties onlyInput = new Properties();
        onlyInput.setProperty("my-model.input", "3");

        assertThatThrownBy(() -> PriceTable.empty().withProperties(onlyOutput))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("my-model.input");
        assertThatThrownBy(() -> PriceTable.empty().withProperties(onlyInput))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("my-model.output");
    }

    @Test
    void unexpected_key_or_value_is_refused() {
        Properties badKey = new Properties();
        badKey.setProperty("my-model.price", "3");
        Properties badValue = new Properties();
        badValue.setProperty("my-model.input", "cheap");
        badValue.setProperty("my-model.output", "3");

        assertThatThrownBy(() -> PriceTable.empty().withProperties(badKey)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PriceTable.empty().withProperties(badValue))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cheap");
    }

    @Test
    void batch_price_is_half_price() {
        assertThat(new ModelPrice(4, 20).batch()).isEqualTo(new ModelPrice(2, 10));
    }

    @Test
    void negative_price_is_refused() {
        assertThatThrownBy(() -> new ModelPrice(-1, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModelPrice(Double.NaN, 2)).isInstanceOf(IllegalArgumentException.class);
    }
}

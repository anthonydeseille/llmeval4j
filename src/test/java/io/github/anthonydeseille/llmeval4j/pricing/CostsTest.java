package io.github.anthonydeseille.llmeval4j.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class CostsTest {

    @Test
    void token_usages_add_up() {
        TokenUsage total = TokenUsage.sum(List.of(new TokenUsage(1_000, 100), new TokenUsage(2_000, 200)));

        assertThat(total).isEqualTo(new TokenUsage(3_000, 300));
        assertThat(total.totalTokens()).isEqualTo(3_300);
        assertThat(TokenUsage.sum(List.of())).isEqualTo(TokenUsage.ZERO);
        assertThatThrownBy(() -> new TokenUsage(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cost_of_a_usage() {
        TokenUsage usage = new TokenUsage(10_000, 1_000);

        assertThat(usage.cost(new ModelPrice(4, 20))).isCloseTo(0.06, within(1e-9));
        assertThat(PriceTable.defaults().cost("claude-opus-5-5", usage)).isCloseTo(0.06, within(1e-9));
        assertThat(PriceTable.defaults().cost("unknown", usage)).isNaN();
    }

    @Test
    void an_unknown_cost_stays_visibly_unknown() {
        assertThat(Dollars.format(0.032)).isEqualTo("$0.03");
        assertThat(Dollars.format(Double.NaN)).isEqualTo("n/a");
        assertThat(Dollars.roundToCents(1.234)).isEqualTo(1.23);
        assertThat(Dollars.roundToCents(Double.NaN)).isNull();
    }

    @Test
    void a_price_describes_itself() {
        assertThat(new ModelPrice(4, 20).describe()).isEqualTo("$4.00 / $20.00 per million tokens (input / output)");
        assertThat(ModelPrice.FREE.describe()).isEqualTo("$0.00 / $0.00 per million tokens (input / output)");
    }
}

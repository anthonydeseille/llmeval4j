package io.github.anthonydeseille.llmeval4j.pricing;

/**
 * Price of a model, in US dollars per million tokens.
 * <p>
 * Reasoning (thinking) tokens are usually billed as output tokens: count them in {@code outputTokens}.
 *
 * @param inputPerMillion  dollars per million input tokens
 * @param outputPerMillion dollars per million output tokens
 */
public record ModelPrice(double inputPerMillion, double outputPerMillion) {

    /** Price of a model that costs nothing, such as a model running locally. */
    public static final ModelPrice FREE = new ModelPrice(0, 0);

    public ModelPrice {
        if (inputPerMillion < 0 || outputPerMillion < 0 || Double.isNaN(inputPerMillion)
                || Double.isNaN(outputPerMillion)) {
            throw new IllegalArgumentException(
                    "Prices must be positive numbers: " + inputPerMillion + " / " + outputPerMillion);
        }
    }

    /** Cost in dollars of a call that used these tokens. */
    public double cost(long inputTokens, long outputTokens) {
        return (inputTokens * inputPerMillion + outputTokens * outputPerMillion) / 1_000_000;
    }

    /** Price through a batch API billed at half price, such as Anthropic's Message Batches API. */
    public ModelPrice batch() {
        return new ModelPrice(inputPerMillion / 2, outputPerMillion / 2);
    }
}

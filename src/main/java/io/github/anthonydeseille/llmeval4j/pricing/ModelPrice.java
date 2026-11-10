package io.github.anthonydeseille.llmeval4j.pricing;

import java.util.Locale;

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

    /**
     * Creates a price.
     *
     * @param inputPerMillion  dollars per million input tokens
     * @param outputPerMillion dollars per million output tokens
     * @throws IllegalArgumentException if a price is negative or not a number
     */
    public ModelPrice {
        if (inputPerMillion < 0 || outputPerMillion < 0 || Double.isNaN(inputPerMillion)
                || Double.isNaN(outputPerMillion)) {
            throw new IllegalArgumentException(
                    "Prices must be positive numbers: " + inputPerMillion + " / " + outputPerMillion);
        }
    }

    /**
     * {@return cost in dollars of a call that used these tokens}
     *
     * @param inputTokens  input tokens of the call
     * @param outputTokens output tokens of the call, reasoning tokens included
     */
    public double cost(long inputTokens, long outputTokens) {
        return (inputTokens * inputPerMillion + outputTokens * outputPerMillion) / 1_000_000;
    }

    /**
     * {@return the price through a batch API billed at half price, such as Anthropic's Message Batches
     * API}
     */
    public ModelPrice batch() {
        return new ModelPrice(inputPerMillion / 2, outputPerMillion / 2);
    }

    /** {@return the price for reports: {@code $4.00 / $20.00 per million tokens (input / output)}} */
    public String describe() {
        return String.format(Locale.ROOT, "$%.2f / $%.2f per million tokens (input / output)", inputPerMillion,
                outputPerMillion);
    }
}

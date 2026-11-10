package io.github.anthonydeseille.llmeval4j.pricing;

import java.util.Collection;

/**
 * Tokens used by one call or summed over many, such as all the questions of an evaluation or all the
 * calls to a judge model.
 *
 * @param inputTokens  input (prompt) tokens
 * @param outputTokens output tokens, reasoning tokens included
 */
public record TokenUsage(long inputTokens, long outputTokens) {

    /** No token at all, the start of a sum. */
    public static final TokenUsage ZERO = new TokenUsage(0, 0);

    /**
     * Creates a usage.
     *
     * @param inputTokens  input (prompt) tokens
     * @param outputTokens output tokens, reasoning tokens included
     * @throws IllegalArgumentException if a count is negative
     */
    public TokenUsage {
        if (inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("Token counts cannot be negative: " + inputTokens + " / " + outputTokens);
        }
    }

    /**
     * {@return the sum of both usages}
     *
     * @param other tokens to add
     */
    public TokenUsage plus(TokenUsage other) {
        return new TokenUsage(inputTokens + other.inputTokens, outputTokens + other.outputTokens);
    }

    /**
     * {@return their sum; {@link #ZERO} for an empty collection}
     *
     * @param usages usages to add up
     */
    public static TokenUsage sum(Collection<TokenUsage> usages) {
        return usages.stream().reduce(ZERO, TokenUsage::plus);
    }

    /** {@return input and output tokens together} */
    public long totalTokens() {
        return inputTokens + outputTokens;
    }

    /**
     * {@return cost in dollars of these tokens at this price}
     *
     * @param price price of the model
     */
    public double cost(ModelPrice price) {
        return price.cost(inputTokens, outputTokens);
    }
}

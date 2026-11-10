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

    public static final TokenUsage ZERO = new TokenUsage(0, 0);

    public TokenUsage {
        if (inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("Token counts cannot be negative: " + inputTokens + " / " + outputTokens);
        }
    }

    public TokenUsage plus(TokenUsage other) {
        return new TokenUsage(inputTokens + other.inputTokens, outputTokens + other.outputTokens);
    }

    public static TokenUsage sum(Collection<TokenUsage> usages) {
        return usages.stream().reduce(ZERO, TokenUsage::plus);
    }

    public long totalTokens() {
        return inputTokens + outputTokens;
    }

    /** Cost in dollars at this price. */
    public double cost(ModelPrice price) {
        return price.cost(inputTokens, outputTokens);
    }
}

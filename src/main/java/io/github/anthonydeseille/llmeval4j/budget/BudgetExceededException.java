package io.github.anthonydeseille.llmeval4j.budget;

import java.util.Locale;

/**
 * Thrown by {@link DailyBudget#check()} when today's budget is spent. A web application would
 * typically turn it into an HTTP 429 response ("try again tomorrow").
 */
public final class BudgetExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final double spentToday;
    private final double dailyLimit;

    public BudgetExceededException(double spentToday, double dailyLimit) {
        super(String.format(Locale.ROOT, "Daily budget reached: $%.2f spent of $%.2f", spentToday, dailyLimit));
        this.spentToday = spentToday;
        this.dailyLimit = dailyLimit;
    }

    public double spentToday() {
        return spentToday;
    }

    public double dailyLimit() {
        return dailyLimit;
    }
}

package io.github.anthonydeseille.llmeval4j.budget;

import java.util.Locale;

/**
 * Thrown by {@link DailyBudget#check()} when today's budget is spent. A web application would
 * typically turn it into an HTTP 429 response ("try again tomorrow").
 */
public final class BudgetExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Dollars spent today when the call was refused. */
    private final double spentToday;
    /** Daily limit, in dollars. */
    private final double dailyLimit;

    /**
     * Creates the exception, with a message giving both amounts.
     *
     * @param spentToday dollars spent today
     * @param dailyLimit daily limit, in dollars
     */
    public BudgetExceededException(double spentToday, double dailyLimit) {
        super(String.format(Locale.ROOT, "Daily budget reached: $%.2f spent of $%.2f", spentToday, dailyLimit));
        this.spentToday = spentToday;
        this.dailyLimit = dailyLimit;
    }

    /** {@return dollars spent today when the call was refused} */
    public double spentToday() {
        return spentToday;
    }

    /** {@return daily limit, in dollars} */
    public double dailyLimit() {
        return dailyLimit;
    }
}

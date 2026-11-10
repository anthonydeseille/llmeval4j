package io.github.anthonydeseille.llmeval4j.budget;

/**
 * Notified of a {@link DailyBudget}'s milestones, to log them, count them in metrics or alert someone.
 * <p>
 * {@link #onWarning} and {@link #onExceeded} are called at most once a day each. Calls happen on the
 * thread that recorded the spending or checked the budget, outside any lock: keep them fast and do not
 * throw.
 */
public interface BudgetListener {

    /** A listener that does nothing. */
    BudgetListener NONE = new BudgetListener() {
    };

    /**
     * Spending reached the warning threshold (80% of the limit by default).
     *
     * @param spentToday dollars spent today
     * @param dailyLimit daily limit, in dollars
     */
    default void onWarning(double spentToday, double dailyLimit) {
    }

    /**
     * Spending reached the limit: calls are refused until tomorrow.
     *
     * @param spentToday dollars spent today
     * @param dailyLimit daily limit, in dollars
     */
    default void onExceeded(double spentToday, double dailyLimit) {
    }

    /**
     * A call was refused because the limit is reached; called on every refusal.
     *
     * @param spentToday dollars spent today
     * @param dailyLimit daily limit, in dollars
     */
    default void onRefused(double spentToday, double dailyLimit) {
    }
}

package io.github.anthonydeseille.llmeval4j.budget;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Money spent today on paid model calls, and a daily cap: once the cap is reached, {@link #check()}
 * refuses new calls until the next day.
 * <p>
 * Typical use, around each call:
 * <pre>{@code
 * budget.check();                                   // throws BudgetExceededException when spent
 * Response response = client.call(...);
 * budget.add(prices.cost(model, inputTokens, outputTokens));
 * }</pre>
 * <p>
 * This is an <b>approximate</b> guard: spending is recorded at the end of each call, not reserved
 * before it. Concurrent calls all pass the check before spending goes up, so the overshoot is bounded
 * by the number of calls in flight times the cost of one call. It complements the monthly spending
 * limit of your provider's console; it does not replace it.
 * <p>
 * The {@link BudgetListener} is told once a day when spending reaches the warning threshold (80% by
 * default) and when it reaches the cap: plug your logs, metrics or alerts there. Spending is kept in
 * memory, resets every day (according to the given clock) and on restart.
 * <p>
 * Thread-safe.
 */
public final class DailyBudget {

    /** Default warning threshold, as a fraction of the cap. */
    public static final double DEFAULT_WARNING_THRESHOLD = 0.8;

    private final double limit;
    private final double warningThreshold;
    private final Clock clock;
    private final BudgetListener listener;
    private LocalDate day;
    private double spent;
    private boolean warned;
    private boolean exceeded;

    private DailyBudget(double limit, double warningThreshold, Clock clock, BudgetListener listener) {
        if (Double.isNaN(limit) || limit < 0) {
            throw new IllegalArgumentException("The daily limit must be a positive number: " + limit);
        }
        if (!(warningThreshold > 0 && warningThreshold <= 1)) {
            throw new IllegalArgumentException("The warning threshold must be in (0, 1]: " + warningThreshold);
        }
        this.limit = limit;
        this.warningThreshold = warningThreshold;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * A budget of {@code dollarsPerDay}, with the system clock in UTC and no listener.
     *
     * @param dollarsPerDay daily limit, in dollars
     * @return the budget
     */
    public static DailyBudget of(double dollarsPerDay) {
        return builder(dollarsPerDay).build();
    }

    /**
     * A budget that tracks spending but never refuses a call: for tests and tools.
     *
     * @return a budget without limit
     */
    public static DailyBudget unlimited() {
        return of(Double.POSITIVE_INFINITY);
    }

    /**
     * Starts a budget with options: warning threshold, clock, listener.
     *
     * @param dollarsPerDay daily limit, in dollars
     * @return a builder
     */
    public static Builder builder(double dollarsPerDay) {
        return new Builder(dollarsPerDay);
    }

    /**
     * Refuses the call if today's budget is spent.
     *
     * @throws BudgetExceededException if today's spending has reached the limit
     */
    public void check() {
        double spentNow;
        synchronized (this) {
            resetOnNewDay();
            if (spent < limit) {
                return;
            }
            spentNow = spent;
        }
        listener.onRefused(spentNow, limit);
        throw new BudgetExceededException(spentNow, limit);
    }

    /**
     * Records the cost of a call. An unknown cost ({@code NaN}, model without a price) or a zero cost
     * is ignored.
     *
     * @param dollars cost of the call, in dollars
     */
    public void add(double dollars) {
        if (Double.isNaN(dollars) || dollars <= 0) {
            return;
        }
        boolean warn = false;
        boolean exceed = false;
        double spentNow;
        synchronized (this) {
            resetOnNewDay();
            spent += dollars;
            spentNow = spent;
            if (!exceeded && spent >= limit) {
                exceeded = true;
                warned = true;
                exceed = true;
            } else if (!warned && spent >= warningThreshold * limit) {
                warned = true;
                warn = true;
            }
        }
        // Outside the lock: a slow listener must not block other calls
        if (exceed) {
            listener.onExceeded(spentNow, limit);
        } else if (warn) {
            listener.onWarning(spentNow, limit);
        }
    }

    /** {@return dollars spent today} */
    public synchronized double spentToday() {
        resetOnNewDay();
        return spent;
    }

    /** {@return dollars left today, never negative; infinite for an {@link #unlimited()} budget} */
    public synchronized double remainingToday() {
        resetOnNewDay();
        return Math.max(0, limit - spent);
    }

    /** {@return daily limit, in dollars} */
    public double limit() {
        return limit;
    }

    private void resetOnNewDay() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            spent = 0;
            warned = false;
            exceeded = false;
        }
    }

    /** Options of a {@link DailyBudget}. */
    public static final class Builder {

        private final double limit;
        private double warningThreshold = DEFAULT_WARNING_THRESHOLD;
        private Clock clock = Clock.systemUTC();
        private BudgetListener listener = BudgetListener.NONE;

        private Builder(double limit) {
            this.limit = limit;
        }

        /**
         * {@return this builder}
         *
         * @param fraction fraction of the cap at which {@link BudgetListener#onWarning} is called, in
         * (0, 1]; 0.8 by default
         */
        public Builder warningThreshold(double fraction) {
            this.warningThreshold = fraction;
            return this;
        }

        /**
         * {@return this builder}
         *
         * @param clock clock that decides when a day starts; {@link Clock#systemUTC()} by default
         */
        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * {@return this builder}
         *
         * @param listener notified at the warning threshold, at the limit and on refusals
         */
        public Builder listener(BudgetListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * {@return the budget}
         *
         * @throws IllegalArgumentException if the limit is negative or the threshold not in (0, 1]
         */
        public DailyBudget build() {
            return new DailyBudget(limit, warningThreshold, clock, listener);
        }
    }
}

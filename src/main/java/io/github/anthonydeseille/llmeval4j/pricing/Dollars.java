package io.github.anthonydeseille.llmeval4j.pricing;

import java.util.Locale;

/**
 * Display and rounding of costs, where an unknown cost ({@code NaN}) stays visibly unknown instead of
 * turning into a misleading zero.
 */
public final class Dollars {

    /** Shown in place of an unknown cost. */
    public static final String UNKNOWN = "n/a";

    private Dollars() {
    }

    /** {@code $0.03}, or {@code n/a} for an unknown cost. */
    public static String format(double dollars) {
        return Double.isNaN(dollars) ? UNKNOWN : String.format(Locale.ROOT, "$%.2f", dollars);
    }

    /**
     * Rounded to the cent, for a history file or a JSON summary; {@code null} for an unknown cost, so
     * that it is written as an empty value rather than {@code NaN} or {@code 0}.
     */
    public static Double roundToCents(double dollars) {
        return Double.isNaN(dollars) ? null : Math.round(dollars * 100) / 100.0;
    }
}

package io.github.anthonydeseille.llmeval4j.regression;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Numbers read from history cells, and their display. */
final class Numbers {

    private Numbers() {
    }

    /** The value as a number; {@code null} if absent, blank or not a number. */
    static Double parse(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            double d = number.doubleValue();
            return Double.isNaN(d) ? null : d;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            double d = Double.parseDouble(text);
            return Double.isNaN(d) ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Removes floating point noise: 0.1 + 0.2 - 0.3 is 0, not 5.5e-17. */
    static double clean(double value) {
        return Math.round(value * 1e9) / 1e9;
    }

    /** At most two decimals, without trailing zeros: {@code 80}, {@code 0.35}, {@code 1.5}. */
    static String format(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** Signed difference: {@code +3}, {@code -0.5}, {@code 0}. */
    static String signed(double delta) {
        return (delta > 0 ? "+" : "") + format(delta);
    }

    /** Signed difference with one decimal: {@code +1.5}, {@code -2.0}. */
    static String signedTenths(double delta) {
        return String.format(Locale.ROOT, "%+.1f", delta);
    }
}

package io.github.anthonydeseille.llmeval4j.regression;

import java.util.Map;
import java.util.Optional;

/** A share given by a numerator and a denominator column, compared in percentage points. */
record RatioMetric(String numeratorColumn, String denominatorColumn, String label, double maxDropPoints)
        implements Metric {

    RatioMetric {
        if (Double.isNaN(maxDropPoints) || maxDropPoints < 0) {
            throw new IllegalArgumentException("The threshold must be a positive number: " + maxDropPoints);
        }
    }

    @Override
    public Optional<Change> compare(Map<String, ?> reference, Map<String, ?> current) {
        Double before = share(reference);
        Double after = share(current);
        if (before == null || after == null) {
            return Optional.empty();
        }
        // Rounded to a tenth of a point: 196/200 against 197/197 gives -2.0, not -2.0000000000000018
        double delta = Math.round(1000 * (after - before)) / 10.0;
        return Optional.of(new Change(label, fraction(reference), fraction(current),
                Numbers.signedTenths(delta) + " pts", delta < -maxDropPoints));
    }

    private Double share(Map<String, ?> row) {
        Double numerator = Numbers.parse(row.get(numeratorColumn));
        Double denominator = Numbers.parse(row.get(denominatorColumn));
        return numerator == null || denominator == null || denominator == 0 ? null : numerator / denominator;
    }

    private String fraction(Map<String, ?> row) {
        return Numbers.format(Numbers.parse(row.get(numeratorColumn))) + "/"
                + Numbers.format(Numbers.parse(row.get(denominatorColumn)));
    }
}

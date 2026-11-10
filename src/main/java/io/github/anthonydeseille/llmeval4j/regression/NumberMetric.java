package io.github.anthonydeseille.llmeval4j.regression;

import java.util.Map;
import java.util.Optional;

/** A single numeric column, compared by difference. */
record NumberMetric(String column, String label, boolean higherIsBetter, double threshold, String valueUnit,
                    String deltaUnit) implements Metric {

    NumberMetric {
        if (Double.isNaN(threshold) || threshold < 0) {
            throw new IllegalArgumentException("The threshold must be a positive number: " + threshold);
        }
    }

    @Override
    public Optional<Change> compare(Map<String, ?> reference, Map<String, ?> current) {
        Double before = Numbers.parse(reference.get(column));
        Double after = Numbers.parse(current.get(column));
        if (before == null || after == null) {
            return Optional.empty();
        }
        double delta = Numbers.clean(after - before);
        boolean regression = higherIsBetter ? delta < -threshold : delta > threshold;
        return Optional.of(new Change(label, Numbers.format(before) + valueUnit, Numbers.format(after) + valueUnit,
                Numbers.signed(delta) + deltaUnit, regression));
    }
}

package io.github.anthonydeseille.llmeval4j.regression;

import io.github.anthonydeseille.llmeval4j.history.EvalHistory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compares an evaluation run with the latest comparable run of the history, metric by metric, and
 * decides whether it regressed.
 * <p>
 * Comparable means having the same values in the {@link Builder#sameValuesOf matching columns}, such
 * as the same model and the same number of questions: otherwise the changes would measure the test
 * set, not your change.
 * <pre>{@code
 * RegressionCheck check = RegressionCheck.builder()
 *         .sameValuesOf("model", "questions")
 *         .metric(Metric.percentage("quality_pct", "Quality (judge)", 5))
 *         .metric(Metric.lowerIsBetter("errors", "Errors", 1))
 *         .build();
 * Comparison comparison = check.compare(EvalHistory.read(file), currentRun);
 * assertThat(comparison.regression()).as(comparison.toMarkdown()).isFalse();
 * }</pre>
 * Instances are immutable.
 */
public final class RegressionCheck {

    private final List<String> matchingColumns;
    private final List<Metric> metrics;

    private RegressionCheck(List<String> matchingColumns, List<Metric> metrics) {
        this.matchingColumns = List.copyOf(matchingColumns);
        this.metrics = List.copyOf(metrics);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Compares {@code current} with the latest row of the history having its matching values. */
    public Comparison compare(EvalHistory history, Map<String, ?> current) {
        return compare(history, current, Map.of());
    }

    /**
     * Same as {@link #compare(EvalHistory, Map)}, with a fallback when the history has no comparable
     * row: the matching values are replaced by {@code fallback} and searched again. For instance, a
     * new model with no history yet is compared with the current model:
     * {@code compare(history, run, Map.of("model", "claude-sonnet-5-5"))}.
     */
    public Comparison compare(EvalHistory history, Map<String, ?> current, Map<String, ?> fallback) {
        Map<String, Object> keys = new LinkedHashMap<>();
        for (String column : matchingColumns) {
            keys.put(column, current.get(column));
        }
        Map<String, String> reference = history.latestWith(keys).orElse(null);
        if (reference == null && !fallback.isEmpty()) {
            keys.putAll(fallback);
            reference = history.latestWith(keys).orElse(null);
        }
        if (reference == null) {
            return new Comparison(null, List.of());
        }
        List<Change> changes = new ArrayList<>();
        for (Metric metric : metrics) {
            metric.compare(reference, current).ifPresent(changes::add);
        }
        return new Comparison(reference, changes);
    }

    /** Options of a {@link RegressionCheck}. */
    public static final class Builder {

        private final List<String> matchingColumns = new ArrayList<>();
        private final List<Metric> metrics = new ArrayList<>();

        private Builder() {
        }

        /** Columns a previous run must share with the current one to be comparable. */
        public Builder sameValuesOf(String... columns) {
            matchingColumns.addAll(List.of(columns));
            return this;
        }

        /** A metric to compare; reports list them in the order they are added. */
        public Builder metric(Metric metric) {
            metrics.add(Objects.requireNonNull(metric, "metric"));
            return this;
        }

        public RegressionCheck build() {
            if (metrics.isEmpty()) {
                throw new IllegalStateException("Add at least one metric");
            }
            return new RegressionCheck(matchingColumns, metrics);
        }
    }
}

package io.github.anthonydeseille.llmeval4j.regression;

import java.util.Map;
import java.util.Optional;

/**
 * An indicator compared between a reference run and the current one, with the threshold beyond which
 * a change is a regression.
 * <p>
 * Evaluations with an LLM are noisy: on 20 questions, one answer losing a key point moves a quality
 * score by a few points with no change at all. Thresholds let that noise through and stop only a
 * <b>clear</b> drop. Pick them by running the same evaluation twice and looking at the spread.
 * <p>
 * Implement this interface for an indicator the factories below do not cover.
 */
public interface Metric {

    /** Name shown in reports. */
    String label();

    /**
     * Change between the two runs; empty if either side lacks the value (judge not run, empty cell):
     * a missing value is not compared, and never fails a build.
     */
    Optional<Change> compare(Map<String, ?> reference, Map<String, ?> current);

    /**
     * A percentage (0 to 100) where higher is better, such as a quality score: a regression if it drops
     * by more than {@code maxDropPoints}.
     */
    static Metric percentage(String column, String label, double maxDropPoints) {
        return new NumberMetric(column, label, true, maxDropPoints, "%", " pts");
    }

    /** A number where higher is better: a regression if it drops by more than {@code maxDrop}. */
    static Metric higherIsBetter(String column, String label, double maxDrop) {
        return new NumberMetric(column, label, true, maxDrop, "", "");
    }

    /**
     * A number where lower is better, such as a count of errors or a cost: a regression if it rises by
     * more than {@code maxIncrease}. Use {@code Double.POSITIVE_INFINITY} to show it without ever
     * failing.
     */
    static Metric lowerIsBetter(String column, String label, double maxIncrease) {
        return new NumberMetric(column, label, false, maxIncrease, "", "");
    }

    /**
     * A share given by two columns, such as verified citations out of all citations, shown as
     * {@code 196/200}: a regression if the share drops by more than {@code maxDropPoints} percentage
     * points. Strict thresholds suit indicators where every miss is a fault, not noise.
     */
    static Metric ratio(String numeratorColumn, String denominatorColumn, String label, double maxDropPoints) {
        return new RatioMetric(numeratorColumn, denominatorColumn, label, maxDropPoints);
    }
}

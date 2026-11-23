package io.github.anthonydeseille.llmeval4j.regression;

/**
 * Change of one metric between the reference run and the current one, formatted for display.
 *
 * @param metric      the metric's label
 * @param reference   value in the reference run
 * @param current     value in the current run
 * @param delta       signed difference, such as {@code -6 pts}
 * @param regression  whether the change goes beyond the metric's threshold in the wrong direction
 * @param informative whether the metric is shown without a threshold ({@link Metric#informative}):
 *                    never a regression
 */
public record Change(String metric, String reference, String current, String delta, boolean regression,
                     boolean informative) {

    /**
     * Checks that an informative change is not a regression.
     *
     * @throws IllegalArgumentException if both {@code regression} and {@code informative} are set
     */
    public Change {
        if (regression && informative) {
            throw new IllegalArgumentException("An informative change cannot be a regression: " + metric);
        }
    }

    /**
     * A change of a metric checked against a threshold (not informative).
     *
     * @param metric     the metric's label
     * @param reference  value in the reference run
     * @param current    value in the current run
     * @param delta      signed difference, such as {@code -6 pts}
     * @param regression whether the change goes beyond the metric's threshold in the wrong direction
     */
    public Change(String metric, String reference, String current, String delta, boolean regression) {
        this(metric, reference, current, delta, regression, false);
    }
}

package io.github.anthonydeseille.llmeval4j.regression;

/**
 * Change of one metric between the reference run and the current one, formatted for display.
 *
 * @param metric     the metric's label
 * @param reference  value in the reference run
 * @param current    value in the current run
 * @param delta      signed difference, such as {@code -6 pts}
 * @param regression whether the change goes beyond the metric's threshold in the wrong direction
 */
public record Change(String metric, String reference, String current, String delta, boolean regression) {
}

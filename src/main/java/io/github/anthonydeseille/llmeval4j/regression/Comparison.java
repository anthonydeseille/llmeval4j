package io.github.anthonydeseille.llmeval4j.regression;

import java.util.List;
import java.util.Map;

/**
 * Result of a {@link RegressionCheck}: the reference run and the change of each metric.
 *
 * @param reference row of the history compared against; {@code null} if no previous run matches, in
 *                  which case there is nothing to compare and nothing fails
 * @param changes   one change per comparable metric, in the order the metrics were declared
 */
public record Comparison(Map<String, String> reference, List<Change> changes) {

    /**
     * Creates a comparison; the list of changes is copied.
     *
     * @param reference row of the history compared against, {@code null} if none matches
     * @param changes   one change per comparable metric
     */
    public Comparison {
        changes = List.copyOf(changes);
    }

    /** {@return whether a previous run was found to compare with} */
    public boolean hasReference() {
        return reference != null;
    }

    /** {@return whether at least one metric regressed beyond its threshold: the build should fail} */
    public boolean regression() {
        return changes.stream().anyMatch(Change::regression);
    }

    /**
     * {@return a Markdown table of the changes and the verdict, for a pull request comment or a job
     * summary}
     */
    public String toMarkdown() {
        if (!hasReference()) {
            return "No previous run to compare with: nothing to check.\n";
        }
        StringBuilder md = new StringBuilder("| Metric | Reference | Now | Change | |\n|---|---|---|---|---|\n");
        for (Change change : changes) {
            md.append("| ").append(change.metric()).append(" | ").append(change.reference()).append(" | ")
                    .append(change.current()).append(" | ").append(change.delta()).append(" | ")
                    .append(change.regression() ? "❌ regression" : "✅").append(" |\n");
        }
        md.append('\n').append(regression()
                ? "**Regression beyond the thresholds.**"
                : "No regression beyond the thresholds.").append('\n');
        return md.toString();
    }
}

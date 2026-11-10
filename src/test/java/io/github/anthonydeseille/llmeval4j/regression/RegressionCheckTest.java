package io.github.anthonydeseille.llmeval4j.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.anthonydeseille.llmeval4j.history.EvalHistory;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegressionCheckTest {

    private static final EvalHistory HISTORY = EvalHistory.parse("""
            date,model,questions,quality_pct,errors,verified,citations,cost_usd
            2026-10-01,claude-sonnet-5-5,20,70,4,180,200,0.50
            2026-10-02,claude-sonnet-5-5,20,80,2,196,200,0.40
            2026-10-03,claude-sonnet-5-5,5,99,0,50,50,0.10
            2026-10-04,claude-opus-5-5,20,,,,,
            """);

    private static final RegressionCheck CHECK = RegressionCheck.builder()
            .sameValuesOf("model", "questions")
            .metric(Metric.percentage("quality_pct", "Quality", 5))
            .metric(Metric.lowerIsBetter("errors", "Errors", 1))
            .metric(Metric.ratio("verified", "citations", "Verified citations", 2))
            .metric(Metric.lowerIsBetter("cost_usd", "Cost ($)", Double.POSITIVE_INFINITY))
            .build();

    @Test
    void compares_with_the_latest_run_of_the_same_model_and_size() {
        Comparison comparison = CHECK.compare(HISTORY, run("claude-sonnet-5-5", 78, 3, 197, 197, 0.45));

        assertThat(comparison.reference()).containsEntry("date", "2026-10-02");
        assertThat(comparison.changes()).containsExactly(
                new Change("Quality", "80%", "78%", "-2 pts", false),
                new Change("Errors", "2", "3", "+1", false),
                new Change("Verified citations", "196/200", "197/197", "+2.0 pts", false),
                new Change("Cost ($)", "0.4", "0.45", "+0.05", false));
        assertThat(comparison.regression()).isFalse();
    }

    @Test
    void a_drop_at_the_threshold_is_noise_beyond_it_a_regression() {
        assertThat(CHECK.compare(HISTORY, run("claude-sonnet-5-5", 75, 2, 196, 200, 0.4)).regression()).isFalse();

        Comparison drop = CHECK.compare(HISTORY, run("claude-sonnet-5-5", 74, 2, 196, 200, 0.4));
        assertThat(drop.regression()).isTrue();
        assertThat(drop.changes().get(0)).isEqualTo(new Change("Quality", "80%", "74%", "-6 pts", true));
    }

    @Test
    void lower_is_better_fails_on_a_rise_beyond_the_threshold() {
        Comparison comparison = CHECK.compare(HISTORY, run("claude-sonnet-5-5", 80, 4, 196, 200, 0.4));

        assertThat(comparison.changes().get(1)).isEqualTo(new Change("Errors", "2", "4", "+2", true));
    }

    @Test
    void ratio_fails_on_a_drop_beyond_the_threshold_in_points() {
        // 98.0% -> 95.5%: -2.5 points
        Comparison comparison = CHECK.compare(HISTORY, run("claude-sonnet-5-5", 80, 2, 191, 200, 0.4));

        assertThat(comparison.changes().get(2))
                .isEqualTo(new Change("Verified citations", "196/200", "191/200", "-2.5 pts", true));
    }

    @Test
    void ratio_exactly_at_the_threshold_is_not_a_regression() {
        // 197/197 -> 196/200 is exactly -2.0 points once floating point noise is removed
        EvalHistory history = EvalHistory.parse("model,questions,verified,citations\nm,20,197,197\n");
        RegressionCheck check = RegressionCheck.builder().sameValuesOf("model", "questions")
                .metric(Metric.ratio("verified", "citations", "Verified", 2)).build();

        Comparison comparison = check.compare(history, Map.of("model", "m", "questions", 20, "verified", 196,
                "citations", 200));

        assertThat(comparison.changes()).containsExactly(new Change("Verified", "197/197", "196/200", "-2.0 pts", false));
    }

    @Test
    void a_missing_value_on_either_side_is_not_compared() {
        Map<String, Object> current = run("claude-sonnet-5-5", 80, 2, 196, 200, 0.4);
        current.put("errors", null);
        current.put("quality_pct", "");

        assertThat(CHECK.compare(HISTORY, current).changes()).extracting(Change::metric)
                .containsExactly("Verified citations", "Cost ($)");
        assertThat(CHECK.compare(HISTORY, run("claude-opus-5-5", 50, 9, 1, 200, 9)).changes()).isEmpty();
    }

    @Test
    void no_comparable_run_means_nothing_to_compare_and_nothing_fails() {
        Comparison comparison = CHECK.compare(HISTORY, run("claude-haiku-5-5", 10, 50, 0, 200, 0.01));

        assertThat(comparison.hasReference()).isFalse();
        assertThat(comparison.regression()).isFalse();
        assertThat(comparison.toMarkdown()).contains("No previous run");
    }

    @Test
    void a_new_model_can_be_compared_with_a_fallback_model() {
        Comparison comparison = CHECK.compare(HISTORY, run("claude-haiku-5-5", 72, 2, 196, 200, 0.05),
                Map.of("model", "claude-sonnet-5-5"));

        assertThat(comparison.reference()).containsEntry("date", "2026-10-02");
        assertThat(comparison.regression()).isTrue();
    }

    @Test
    void the_fallback_is_not_used_when_the_model_has_a_history() {
        Comparison comparison = CHECK.compare(HISTORY, run("claude-sonnet-5-5", 80, 2, 196, 200, 0.4),
                Map.of("model", "claude-opus-5-5"));

        assertThat(comparison.reference()).containsEntry("date", "2026-10-02");
    }

    @Test
    void markdown_shows_each_change_and_the_verdict() {
        String markdown = CHECK.compare(HISTORY, run("claude-sonnet-5-5", 74, 2, 196, 200, 0.4)).toMarkdown();

        assertThat(markdown)
                .contains("| Metric | Reference | Now | Change | |")
                .contains("| Quality | 80% | 74% | -6 pts | ❌ regression |")
                .contains("| Errors | 2 | 2 | 0 | ✅ |")
                .contains("**Regression beyond the thresholds.**");
    }

    @Test
    void a_custom_metric_can_be_plugged_in() {
        Metric answered = new Metric() {
            @Override
            public String label() {
                return "All answered";
            }

            @Override
            public Optional<Change> compare(Map<String, ?> reference, Map<String, ?> current) {
                boolean failed = !"0".equals(String.valueOf(current.get("errors")));
                return Optional.of(new Change(label(), "", "", "", failed));
            }
        };
        RegressionCheck check = RegressionCheck.builder().sameValuesOf("model", "questions").metric(answered).build();

        assertThat(check.compare(HISTORY, run("claude-sonnet-5-5", 80, 1, 0, 0, 0)).regression()).isTrue();
    }

    @Test
    void a_check_needs_a_metric_and_positive_thresholds() {
        assertThatThrownBy(() -> RegressionCheck.builder().build()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> Metric.percentage("q", "Q", -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decimals_are_shown_without_floating_point_noise() {
        EvalHistory history = EvalHistory.parse("score\n0.3\n");
        RegressionCheck check = RegressionCheck.builder()
                .metric(Metric.higherIsBetter("score", "Score", 0.5)).build();
        Map<String, Object> current = new HashMap<>();
        current.put("score", 0.1 + 0.2);

        assertThat(check.compare(history, current).changes())
                .containsExactly(new Change("Score", "0.3", "0.3", "0", false));
    }

    private static Map<String, Object> run(String model, int quality, int errors, int verified, int citations,
                                           double cost) {
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("model", model);
        run.put("questions", 20);
        run.put("quality_pct", quality);
        run.put("errors", errors);
        run.put("verified", verified);
        run.put("citations", citations);
        run.put("cost_usd", cost);
        return run;
    }
}

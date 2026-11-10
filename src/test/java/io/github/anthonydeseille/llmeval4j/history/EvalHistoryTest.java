package io.github.anthonydeseille.llmeval4j.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvalHistoryTest {

    private static final String CSV = """
            date,model,questions,quality_pct,comment
            2026-10-01,claude-sonnet-5-5,20,80,first run
            2026-10-02,claude-opus-5-5,20,88,"bigger model, better"
            2026-10-03,claude-sonnet-5-5,20,82,"prompt ""v2""\"
            2026-10-04,claude-sonnet-5-5,5,90
            """;

    @Test
    void parses_rows_by_column_name_with_quoted_values_and_missing_cells() {
        EvalHistory history = EvalHistory.parse(CSV);

        assertThat(history.columns()).containsExactly("date", "model", "questions", "quality_pct", "comment");
        assertThat(history.rows()).hasSize(4);
        assertThat(history.rows().get(1)).containsEntry("comment", "bigger model, better");
        assertThat(history.rows().get(2)).containsEntry("comment", "prompt \"v2\"");
        assertThat(history.rows().get(3)).containsEntry("comment", "");
    }

    @Test
    void latest_with_finds_the_most_recent_matching_row() {
        EvalHistory history = EvalHistory.parse(CSV);

        assertThat(history.latestWith(Map.of("model", "claude-sonnet-5-5", "questions", 20)))
                .hasValueSatisfying(row -> assertThat(row).containsEntry("date", "2026-10-03"));
        assertThat(history.latestWith(Map.of("model", "claude-haiku-5-5"))).isEmpty();
    }

    @Test
    void an_empty_text_is_an_empty_history() {
        assertThat(EvalHistory.parse("").rows()).isEmpty();
        assertThat(EvalHistory.parse("a,b\n").rows()).isEmpty();
    }

    @Test
    void line_quotes_commas_and_quotes_and_leaves_null_empty() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("a", "x, y");
        row.put("b", "say \"hi\"");
        row.put("c", null);
        row.put("d", 0.35);
        row.put("e", "two\nlines");

        assertThat(EvalHistory.line(row)).isEqualTo("\"x, y\",\"say \"\"hi\"\"\",,0.35,two lines");
    }

    @Test
    void a_written_row_reads_back_identical() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("comment", "a, \"b\"");
        row.put("cost", 1.5);

        EvalHistory history = EvalHistory.parse(EvalHistory.header(List.copyOf(row.keySet())) + "\n" + EvalHistory.line(row));

        assertThat(history.rows().get(0)).containsEntry("comment", "a, \"b\"").containsEntry("cost", "1.5");
    }

    @Test
    void append_creates_the_file_with_a_header_then_adds_rows(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("evals/history.csv");

        EvalHistory.append(file, row("2026-10-01", 80));
        EvalHistory.append(file, row("2026-10-02", 85));

        assertThat(Files.readString(file, StandardCharsets.UTF_8))
                .isEqualTo("date,quality_pct\n2026-10-01,80\n2026-10-02,85\n");
        assertThat(EvalHistory.read(file).rows()).hasSize(2);
    }

    @Test
    void append_adds_a_line_break_missing_at_the_end_of_the_file(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("history.csv");
        Files.writeString(file, "date,quality_pct\n2026-10-01,80");

        EvalHistory.append(file, row("2026-10-02", 85));

        assertThat(Files.readString(file)).isEqualTo("date,quality_pct\n2026-10-01,80\n2026-10-02,85\n");
    }

    @Test
    void append_refuses_a_row_with_other_columns(@TempDir Path dir) {
        Path file = dir.resolve("history.csv");
        EvalHistory.append(file, row("2026-10-01", 80));

        assertThatThrownBy(() -> EvalHistory.append(file, Map.of("date", "2026-10-02")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Columns differ");
    }

    @Test
    void reading_an_absent_file_gives_an_empty_history(@TempDir Path dir) {
        assertThat(EvalHistory.read(dir.resolve("none.csv")).rows()).isEmpty();
    }

    private static Map<String, Object> row(String date, int quality) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", date);
        row.put("quality_pct", quality);
        return row;
    }
}

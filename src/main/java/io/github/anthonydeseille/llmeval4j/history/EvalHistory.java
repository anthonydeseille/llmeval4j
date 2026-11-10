package io.github.anthonydeseille.llmeval4j.history;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * History of evaluations in a CSV file, one row per run, committed next to the code: each change to
 * a prompt or a model leaves a trace of what it did to the scores and the cost.
 * <p>
 * The columns are yours (model, prompt versions, scores, tokens, cost, a free comment...). A row is
 * a map from column name to value, in column order; a {@code null} value is written as an empty cell
 * and read back as an empty string.
 * <p>
 * Instances are immutable.
 */
public final class EvalHistory {

    private final List<String> columns;
    private final List<Map<String, String>> rows;

    private EvalHistory(List<String> columns, List<Map<String, String>> rows) {
        this.columns = List.copyOf(columns);
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
    }

    /**
     * Parses a CSV text whose first line is the header. Values may be quoted ({@code "a, b"}, with
     * {@code ""} for a quote inside). A row shorter than the header gets empty values.
     *
     * @param csv CSV text, header included
     * @return the history
     */
    public static EvalHistory parse(String csv) {
        List<List<String>> lines = new ArrayList<>();
        for (String line : csv.split("\\R")) {
            if (!line.isBlank()) {
                lines.add(Csv.fields(line));
            }
        }
        if (lines.isEmpty()) {
            return new EvalHistory(List.of(), List.of());
        }
        List<String> header = lines.get(0);
        List<Map<String, String>> rows = new ArrayList<>();
        for (List<String> values : lines.subList(1, lines.size())) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) {
                row.put(header.get(i), i < values.size() ? values.get(i) : "");
            }
            rows.add(Collections.unmodifiableMap(row));
        }
        return new EvalHistory(header, rows);
    }

    /**
     * Reads a history file; an absent file is an empty history.
     *
     * @param file the CSV file
     * @return the history
     * @throws java.io.UncheckedIOException if the file cannot be read
     */
    public static EvalHistory read(Path file) {
        if (!Files.exists(file)) {
            return new EvalHistory(List.of(), List.of());
        }
        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Appends a row to a history file, creating it with a header made of the row's keys if it does not
     * exist yet.
     *
     * @param file the CSV file
     * @param row  column names and values, in column order
     * @throws java.io.UncheckedIOException if the file cannot be read or written
     * @throws IllegalArgumentException if the file exists with other columns: rewrite its header (and
     *                                  pad its rows) before adding a column
     */
    public static void append(Path file, Map<String, ?> row) {
        List<String> keys = List.copyOf(row.keySet());
        EvalHistory existing = read(file);
        try {
            if (existing.columns.isEmpty()) {
                Path parent = file.toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(file, header(keys) + "\n" + line(row) + "\n", StandardCharsets.UTF_8);
                return;
            }
            if (!existing.columns.equals(keys)) {
                throw new IllegalArgumentException("Columns differ from the header of " + file + ": expected "
                        + existing.columns + ", got " + keys);
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            String separator = content.isEmpty() || content.endsWith("\n") ? "" : "\n";
            Files.writeString(file, separator + line(row) + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * {@return the header line, without line break}
     *
     * @param columns column names
     */
    public static String header(List<String> columns) {
        return columns.stream().map(Csv::escape).collect(Collectors.joining(","));
    }

    /**
     * {@return the CSV line of the values, without line break; {@code null} gives an empty cell}
     *
     * @param row column names and values, in column order
     */
    public static String line(Map<String, ?> row) {
        return row.values().stream().map(v -> v == null ? "" : Csv.escape(v.toString()))
                .collect(Collectors.joining(","));
    }

    /** {@return column names, in file order} */
    public List<String> columns() {
        return columns;
    }

    /** {@return rows, oldest first, as maps from column name to value} */
    public List<Map<String, String>> rows() {
        return rows;
    }

    /**
     * {@return the most recent row (the last one in the file) that matches, if any}
     *
     * @param filter condition on a row
     */
    public Optional<Map<String, String>> latest(Predicate<Map<String, String>> filter) {
        for (int i = rows.size() - 1; i >= 0; i--) {
            if (filter.test(rows.get(i))) {
                return Optional.of(rows.get(i));
            }
        }
        return Optional.empty();
    }

    /**
     * The most recent row having the given values, such as the same model and the same number of
     * questions. Values are compared as text, so {@code 20} matches {@code "20"}.
     *
     * @param values column names and the values they must have
     * @return the most recent matching row, if any
     */
    public Optional<Map<String, String>> latestWith(Map<String, ?> values) {
        return latest(row -> values.entrySet().stream()
                .allMatch(e -> String.valueOf(e.getValue()).equals(row.get(e.getKey()))));
    }
}

package io.github.anthonydeseille.llmeval4j.history;

import java.util.ArrayList;
import java.util.List;

/** Just enough CSV (RFC 4180, one record per line) for an evaluation history. */
final class Csv {

    private Csv() {
    }

    /** Fields of one line; quoted fields may contain commas and doubled quotes. */
    static List<String> fields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /**
     * A value ready for a CSV cell: quoted if it contains a comma or a quote. Line breaks become
     * spaces, since a history keeps one run per line.
     */
    static String escape(String value) {
        String flat = value.replaceAll("\\R", " ");
        return flat.contains(",") || flat.contains("\"") ? "\"" + flat.replace("\"", "\"\"") + "\"" : flat;
    }
}

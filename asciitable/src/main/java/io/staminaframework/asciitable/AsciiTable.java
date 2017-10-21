/*
 * Copyright (c) 2017 Stamina Framework developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.staminaframework.asciitable;

import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Table pretty printer.
 * <p>
 * Use this class to pretty print a table:
 * <p>
 * <code>AsciiTable.of(Arrays.asList("COLUMN 1", "COLUMN 2", "COLUMN 3"), rows).render(System.out)</code>
 * <p>
 * where <code>rows</code> is a multi-level of string {@link List}.
 *
 * @author Stamina Framework developers
 */
public final class AsciiTable {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private final List<String> columns;
    private final List<List<String>> rows = new ArrayList<>(4);

    private AsciiTable(final List<String> columns) {
        requireNonNull(columns, "Columns cannot be null");
        this.columns = columns;
    }

    /**
     * Build a table from columns and rows.
     *
     * @param columns columns to add
     * @param rows    rows to print
     * @return a table instance
     */
    public static AsciiTable of(List<String> columns, List<List<String>> rows) {
        final AsciiTable table = of(columns);
        rows.stream().forEach(table::add);
        return table;
    }

    /**
     * Build a table from columns.
     * Rows can be added with {@link #add(List)}.
     *
     * @param columns columns to add
     * @return a table instance
     */
    public static AsciiTable of(List<String> columns) {
        return new AsciiTable(columns);
    }

    /**
     * Add a row.
     *
     * @param row row to add
     */
    public AsciiTable add(List<String> row) {
        requireNonNull(row, "Row cannot be null");
        rows.add(row);
        return this;
    }

    /**
     * Render this table.
     *
     * @param out output stream where this table should be rendered
     */
    public void render(PrintStream out) {
        final Map<Integer, Integer> colWidths = new HashMap<>(rows.size());

        for (int c = 0; c < columns.size(); ++c) {
            int maxWidth = 0;
            if (columns.get(c) != null) {
                maxWidth = columns.get(c).length();
            }
            for (final List<String> row : rows) {
                final String cell = row.get(c);
                if (cell != null) {
                    maxWidth = Math.max(maxWidth, cell.length());
                }
            }
            colWidths.put(c, maxWidth);
        }

        // Print header.
        for (int c = 0; c < columns.size(); ++c) {
            String col = columns.get(c);
            if (col == null) {
                col = "";
            }
            if (c != 0) {
                out.print("|");
            }
            if (c == columns.size() - 1) {
                out.print(col);
            } else {
                final int w = colWidths.get(c);
                out.print(left(col, w));
            }
        }
        out.println();

        // Print rows.
        if (rows.isEmpty()) {
            int totalWidth = colWidths.size() - 1;
            for (final Integer width : colWidths.values()) {
                totalWidth += width;
            }

            String text = center("<empty>", totalWidth);
            // Remove useless spaces at the end of this String.
            text = text.substring(0, text.indexOf(">") + 1);
            out.println(text);
        } else {
            for (final List<String> row : rows) {
                for (int c = 0; c < columns.size(); ++c) {
                    String cell = row.get(c);
                    if (cell == null) {
                        cell = "";
                    }
                    if (c != 0) {
                        out.print("|");
                    }
                    if (c == columns.size() - 1) {
                        out.print(cell);
                    } else {
                        final int w = colWidths.get(c);
                        final Matcher m = NUMBER_PATTERN.matcher(cell);
                        if (m.matches()) {
                            out.print(right(cell, w));
                        } else {
                            out.print(left(cell, w));
                        }
                    }
                }
                out.println();
            }
        }
        out.flush();
    }

    private static String center(String text, int len) {
        String out = String.format("%" + len + "s%s%" + len + "s", "", text, "");
        float mid = (out.length() / 2);
        float start = mid - (len / 2);
        float end = start + len;
        return out.substring((int) start, (int) end);
    }

    private static String left(String text, int len) {
        return String.format("%-" + len + "s", text);
    }

    private static String right(String text, int len) {
        return String.format("%" + len + "s", text);
    }

    public static void main(String[] args) {
        final List<String> cols = Arrays.asList("ID", "STATE", "LEVEL", "NAME");
        AsciiTable.of(cols)
                .add(Arrays.asList("0", "Active", "0", "System Bundle"))
                .add(Arrays.asList("1", "Installed", "1", "OPS4J Pax Logging - API"))
                .render(System.out);
    }
}

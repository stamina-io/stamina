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

package io.staminaframework.runtime.asciitable;

import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * {@link AsciiTable} test.
 *
 * @author Stamina Framework developers
 */
public class AsciiTableTest {
    @Test
    public void testTable() throws IOException {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
        AsciiTable.of(asList("ID", "STATE", "LEVEL", "NAME"))
                .add(asList("0", "Active", "0", "System Bundle"))
                .add(asList("1", "Installed", "1", "OPS4J Pax Logging - API"))
                .render(new PrintStream(buf));
        assertEquals(readLines("testTable.txt"), readLines(buf.toByteArray()));
    }

    @Test
    public void testTableInline() throws IOException {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
        AsciiTable.of(asList("ID", "STATE", "LEVEL", "NAME"),
                asList(
                        asList("0", "Active", "0", "System Bundle"),
                        asList("1", "Installed", "1", "OPS4J Pax Logging - API")
                ))
                .render(new PrintStream(buf));
        assertEquals(readLines("testTable.txt"), readLines(buf.toByteArray()));
    }

    @Test
    public void testTableWithNullCells() throws IOException {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
        AsciiTable.of(asList("ID", "STATE", "LEVEL", "NAME"))
                .add(asList("0", null, "0", "System Bundle"))
                .add(asList("1", "Installed", "1", null))
                .render(new PrintStream(buf));
        assertEquals(readLines("testTableWithNullCells.txt"), readLines(buf.toByteArray()));
    }

    @Test
    public void testTableWithNoRows() throws IOException {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
        AsciiTable.of(asList("ID", "STATE", "LEVEL", "NAME"))
                .render(new PrintStream(buf));
        assertEquals(readLines("testTableWithNoRows.txt"), readLines(buf.toByteArray()));
    }

    @Test(expected = NullPointerException.class)
    public void testTableNullColumns() {
        AsciiTable.of(null);
    }

    private List<String> readLines(byte[] byteArray) throws IOException {
        final List<String> lines = new ArrayList<>(8);
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(byteArray)))) {
            for (String line; (line = in.readLine()) != null; ) {
                lines.add(line);
            }
        }
        return lines;
    }

    private List<String> readLines(String path) throws IOException {
        final List<String> lines = new ArrayList<>(8);
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path)))) {
            for (String line; (line = in.readLine()) != null; ) {
                lines.add(line);
            }
        }
        return lines;
    }
}

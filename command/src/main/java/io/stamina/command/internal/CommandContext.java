/*
 * Copyright (c) 2017 Stamina.io developers.
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

package io.stamina.command.internal;

import io.stamina.command.Command;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

final class CommandContext implements Command.Context {
    private final String[] arguments;
    private final String workingDirectory;
    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;

    public CommandContext(final String[] arguments, final String workingDirectory,
                          final InputStream in, final PrintStream out, final PrintStream err) {
        this.arguments = arguments;
        this.workingDirectory = workingDirectory;
        this.in = in;
        this.out = out;
        this.err = err;
    }

    @Override
    public String[] arguments() {
        return arguments;
    }

    @Override
    public String workingDirectory() {
        return workingDirectory;
    }

    @Override
    public InputStream in() {
        return in;
    }

    @Override
    public PrintStream out() {
        return out;
    }

    @Override
    public PrintStream err() {
        return err;
    }

    @Override
    public String toString() {
        return "arguments=" + Arrays.toString(arguments) + ", workingDirectory=" + workingDirectory;
    }
}

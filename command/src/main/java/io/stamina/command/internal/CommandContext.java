/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
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

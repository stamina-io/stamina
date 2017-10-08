/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.boot.helper.internal;

import io.stamina.boot.helper.CommandLine;

/**
 * {@link io.stamina.boot.helper.CommandLine} internal implementation.
 *
 * @author Stamina developers
 */
class CommandLineImpl implements CommandLine {
    private final String command;
    private final String[] arguments;
    private final String workingDirectory;

    public CommandLineImpl(final String workingDirectory,
                           final String command, final String[] arguments) {
        this.workingDirectory = workingDirectory;
        this.command = command;
        this.arguments = arguments;
    }

    @Override
    public String command() {
        return command;
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
    public String toString() {
        final StringBuilder buf = new StringBuilder(command);
        if (arguments.length != 0) {
            for (final String arg : arguments) {
                buf.append(' ').append(arg);
            }
        }
        return buf.toString();
    }
}

/*
 * Copyright (c) 2017 Stamina developers.
 * All rights reserved.
 */

package io.stamina.command;

import org.osgi.annotation.versioning.ConsumerType;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Command service interface.
 *
 * @author Stamina developers
 */
@ConsumerType
public interface Command {
    /**
     * Print command help.
     *
     * @param out command output stream
     */
    void help(PrintStream out);

    /**
     * Execute this command.
     *
     * @param arguments command arguments, empty if none
     * @param in        command input stream
     * @param out       command output stream
     * @param err       command error stream
     * @throws Exception if command execution failed
     */
    void execute(String[] arguments, InputStream in, PrintStream out, PrintStream err) throws Exception;
}

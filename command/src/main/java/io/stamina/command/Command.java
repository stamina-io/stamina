/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.command;

import org.osgi.annotation.versioning.ConsumerType;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Command service interface.
 *
 * @author Stamina.io developers
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
     * @param context command context
     * @throws Exception if command execution failed
     */
    void execute(Context context) throws Exception;

    /**
     * Command context interface.
     */
    interface Context {
        /**
         * Get command arguments.
         *
         * @return command arguments, empty if none
         */
        String[] arguments();

        /**
         * Get working directory.
         *
         * @return working directory
         */
        String workingDirectory();

        /**
         * Get command input.
         *
         * @return command input
         */
        InputStream in();

        /**
         * Get command output.
         *
         * @return command output
         */
        PrintStream out();

        /**
         * Get command error output.
         *
         * @return command error output
         */
        PrintStream err();
    }
}

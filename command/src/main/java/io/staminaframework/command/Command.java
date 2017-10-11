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

package io.staminaframework.command;

import org.osgi.annotation.versioning.ConsumerType;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Command service interface.
 *
 * @author Stamina Framework developers
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

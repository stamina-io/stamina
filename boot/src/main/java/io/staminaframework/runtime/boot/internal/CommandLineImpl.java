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

package io.staminaframework.runtime.boot.internal;

import io.staminaframework.runtime.boot.CommandLine;

/**
 * {@link CommandLine} internal implementation.
 *
 * @author Stamina Framework developers
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

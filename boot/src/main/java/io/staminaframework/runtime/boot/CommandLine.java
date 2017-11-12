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

package io.staminaframework.runtime.boot;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Command-line interface, holding information about
 * the command and its arguments when this process was invoked.
 * <p>
 * An instance of this interface is automatically published to the Service Registry,
 * provided a command is set.
 * <p>
 * Given this invocation: <code>$ stamina --stamina.log.level=0 run hello:greetings</code>,
 * an instance of this interface will hold the following values:
 * <ul>
 * <li>command: <code>run</code></li>
 * <li>arguments: <code>[ "hello:greetings" ]</code></li>
 * </ul>
 *
 * @author Stamina Framework developers
 */
@ProviderType
public interface CommandLine {
    /**
     * Get command invoked from command-line.
     *
     * @return command, never <code>null</code>
     */
    String command();

    /**
     * Get command arguments, if any.
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
}

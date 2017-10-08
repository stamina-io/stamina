/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.boot.helper;

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
 * @author Stamina developers
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

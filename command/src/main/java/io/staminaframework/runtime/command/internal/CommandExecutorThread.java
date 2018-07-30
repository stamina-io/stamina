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

package io.staminaframework.runtime.command.internal;

import io.staminaframework.runtime.boot.CommandLine;
import io.staminaframework.runtime.command.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.log.Logger;
import org.osgi.util.tracker.ServiceTracker;

import java.util.concurrent.TimeUnit;

class CommandExecutorThread extends Thread {
    private final long commandTimeout;
    private final CommandLine commandLine;
    private final ServiceTracker<Command, Command> commandTracker;
    private final Bundle systemBundle;
    private final Logger logger;

    public CommandExecutorThread(final long commandTimeout,
                                 final CommandLine commandLine,
                                 final ServiceTracker<Command, Command> commandTracker,
                                 final Bundle systemBundle,
                                 final Logger logger) {
        super("Stamina Command Executor Thread");
        setPriority(Thread.MIN_PRIORITY);
        setDaemon(false);
        this.commandTimeout = commandTimeout;
        this.commandLine = commandLine;
        this.commandTracker = commandTracker;
        this.systemBundle = systemBundle;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            // Waiting for a command.
            logger.info("Waiting for command: {}", commandLine.command());
            final Command cmd = commandTracker.waitForService(TimeUnit.SECONDS.toMillis(commandTimeout));
            if (cmd != null) {
                final CommandContext ctx = new CommandContext(commandLine.arguments(), commandLine.workingDirectory(),
                        System.in, System.out, System.err);
                boolean keepPlatformRunning = false;
                try {
                    // Got one command: let's go!
                    logger.info("Executing command-line: $ {}", commandLine);
                    keepPlatformRunning = cmd.execute(ctx);
                } catch (Exception e) {
                    logger.error("Command execution failed", e);
                } finally {
                    // Stop framework after command execution.
                    if (!keepPlatformRunning) {
                        stopFramework();
                    }
                }
            } else {
                logger.error("Command not found: {}", commandLine.command());
                stopFramework();
            }
        } catch (InterruptedException e) {
            logger.debug("Command executor thread interrupted");
        }
    }

    private void stopFramework() {
        try {
            systemBundle.stop();
        } catch (BundleException e) {
            logger.error("Failed to stop system bundle", e);
        }
    }
}

/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.command.internal;

import io.stamina.boot.helper.CommandLine;
import io.stamina.command.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import java.util.concurrent.TimeUnit;

class CommandExecutorThread extends Thread {
    private final long commandTimeout;
    private final CommandLine commandLine;
    private final ServiceTracker<Command, Command> commandTracker;
    private final Bundle systemBundle;
    private final LogService logService;

    public CommandExecutorThread(final long commandTimeout,
                                 final CommandLine commandLine,
                                 final ServiceTracker<Command, Command> commandTracker,
                                 final Bundle systemBundle,
                                 final LogService logService) {
        super("Command Executor Thread");
        setPriority(Thread.MIN_PRIORITY);
        setDaemon(false);
        this.commandTimeout = commandTimeout;
        this.commandLine = commandLine;
        this.commandTracker = commandTracker;
        this.systemBundle = systemBundle;
        this.logService = logService;
    }

    @Override
    public void run() {
        try {
            // Waiting for a command.
            logService.log(LogService.LOG_INFO, "Waiting for command: " + commandLine.command());
            final Command cmd = commandTracker.waitForService(TimeUnit.SECONDS.toMillis(commandTimeout));
            if (cmd != null) {
                try {
                    // Got one command: let's go!
                    logService.log(LogService.LOG_INFO, "Executing command-line: $ " + commandLine);
                    cmd.execute(commandLine.arguments(), System.in, System.out, System.err);
                } catch (Exception e) {
                    logService.log(LogService.LOG_ERROR, "Command execution failed", e);
                } finally {
                    // Stop framework after command execution.
                    stopFramework();
                }
            } else {
                logService.log(LogService.LOG_ERROR, "Command not found: " + commandLine.command());
                stopFramework();
            }
        } catch (InterruptedException e) {
            logService.log(LogService.LOG_DEBUG, "Command executor thread interrupted");
        } finally {
            commandTracker.close();
        }
    }

    private void stopFramework() {
        try {
            systemBundle.stop();
        } catch (BundleException e) {
            logService.log(LogService.LOG_ERROR, "Failed to stop system bundle", e);
        }
    }
}

/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.command.internal;

import io.stamina.boot.helper.CommandLine;
import io.stamina.command.Command;
import io.stamina.command.CommandConstants;
import org.osgi.framework.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * {@link io.stamina.boot.helper.CommandLine} executor component.
 *
 * @author Stamina.io developers
 */
@Component(configurationPid = "io.stamina.command")
public class CommandLineExecutor {
    @interface Config {
        /**
         * How long do we wait for a command?
         *
         * @return timeout in seconds
         */
        long timeout() default 0L;
    }

    @Reference
    private LogService logService;
    @Reference
    private CommandLine commandLine;
    private ServiceTracker<Command, Command> commandTracker;
    private Thread executor;

    @Activate
    public void activate(BundleContext ctx, Config config) throws InvalidSyntaxException {
        final Filter filter = ctx.createFilter("(&(" + Constants.OBJECTCLASS + "=" + Command.class.getName()
                + ")(" + CommandConstants.COMMAND_PROPERTY + "=" + commandLine.command() + "))");
        commandTracker = new ServiceTracker<>(ctx, filter, null);
        commandTracker.open();

        // Start a new thread handling command execution.
        final Bundle systemBundle = ctx.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        executor = new CommandExecutorThread(config.timeout(), commandLine, commandTracker, systemBundle, logService);
        executor.start();
    }

    @Deactivate
    public void deactivate() {
        if (commandTracker != null) {
            // Closing ServiceTracker should be enough to "unlock" command executor thread.
            commandTracker.close();
            commandTracker = null;
        }
        if (executor != null) {
            try {
                executor.join(10000);
            } catch (InterruptedException e) {
            }
            executor = null;
        }
    }

}

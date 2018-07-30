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
import io.staminaframework.runtime.command.CommandConstants;
import org.osgi.framework.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * {@link CommandLine} executor component.
 *
 * @author Stamina Framework developers
 */
@Component(configurationPid = "io.staminaframework.runtime.command")
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
    private LoggerFactory loggerFactory;
    @Reference
    private CommandLine commandLine;
    private ServiceTracker<Command, Command> commandTracker;
    private Thread executor;

    @Activate
    void activate(BundleContext ctx, Config config) throws InvalidSyntaxException {
        final Logger logger = loggerFactory.getLogger(getClass());

        final Filter filter = ctx.createFilter("(&(" + Constants.OBJECTCLASS + "=" + Command.class.getName()
                + ")(" + CommandConstants.COMMAND + "=" + commandLine.command() + "))");
        commandTracker = new ServiceTracker<>(ctx, filter, null);
        commandTracker.open();

        // Start a new thread handling command execution.
        final Bundle systemBundle = ctx.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        executor = new CommandExecutorThread(config.timeout(), commandLine, commandTracker, systemBundle, logger);
        executor.start();
    }

    @Deactivate
    void deactivate() {
        if (commandTracker != null) {
            // Closing ServiceTracker should be enough to "unlock" command executor thread.
            commandTracker.close();
            commandTracker = null;
        }
        if (executor != null) {
            executor.interrupt();
            try {
                executor.join(10000);
            } catch (InterruptedException e) {
            }
            executor = null;
        }
    }

}

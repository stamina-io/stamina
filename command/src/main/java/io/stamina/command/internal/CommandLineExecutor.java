/*
 * Copyright (c) 2017 Stamina.io developers.
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

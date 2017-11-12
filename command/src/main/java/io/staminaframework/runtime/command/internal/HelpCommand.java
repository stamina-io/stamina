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

import io.staminaframework.runtime.command.Command;
import io.staminaframework.runtime.command.CommandConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Command printing help about an other command.
 *
 * @author Stamina Framework developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND + "=help")
public class HelpCommand implements Command {
    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        bundleContext = null;
    }

    @Override
    public void help(PrintStream out) {
        out.println("Show command help.");
        out.println("If no command is set, show available commands.");
        out.println("Usage: help [<command name>]");
    }

    @Override
    public boolean execute(Context ctx) throws Exception {
        final String cmdName = ctx.arguments().length == 0 ? null : ctx.arguments()[0];
        if (cmdName == null) {
            final List<String> commandNames = new CopyOnWriteArrayList<>();
            final ServiceTracker<Command, Command> tracker =
                    new ServiceTracker<>(bundleContext, Command.class, new ServiceTrackerCustomizer<Command, Command>() {
                        @Override
                        public Command addingService(ServiceReference<Command> reference) {
                            final String name = (String) reference.getProperty(CommandConstants.COMMAND);
                            if (name != null) {
                                commandNames.add(name);
                            }
                            return null;
                        }

                        @Override
                        public void modifiedService(ServiceReference<Command> reference, Command service) {
                        }

                        @Override
                        public void removedService(ServiceReference<Command> reference, Command service) {
                        }
                    });
            tracker.open();
            try {
                synchronized (tracker) {
                    tracker.wait(4000);
                }
            } finally {
                tracker.close();
            }

            ctx.out().println("Available commands:");
            commandNames.stream().sorted().distinct().forEach(ctx.out()::println);
        } else {
            final String commandFilter = "(&(" + Constants.OBJECTCLASS + "=" + Command.class.getName()
                    + ")(" + CommandConstants.COMMAND + "=" + cmdName + "))";
            final ServiceTracker<Command, Command> tracker =
                    new ServiceTracker<>(bundleContext, bundleContext.createFilter(commandFilter), null);
            tracker.open();
            try {
                final Command cmd = tracker.waitForService(4000);
                if (cmd == null) {
                    ctx.err().println("Command not found: " + cmdName);
                } else {
                    cmd.help(ctx.out());
                }
            } finally {
                tracker.close();
            }
        }
        return false;
    }
}

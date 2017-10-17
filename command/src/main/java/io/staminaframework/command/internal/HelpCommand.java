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

package io.staminaframework.command.internal;

import io.staminaframework.command.Command;
import io.staminaframework.command.CommandConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    public void execute(Context ctx) throws Exception {
        final String cmdName = ctx.arguments().length == 0 ? null : ctx.arguments()[0];
        if (cmdName == null) {
            final Collection<ServiceReference<Command>> commandRefs =
                    bundleContext.getServiceReferences(Command.class, null);
            final List<String> commandNames = new ArrayList<>(commandRefs.size());
            for (final ServiceReference<Command> ref : commandRefs) {
                final Object rawCommandName = ref.getProperty(CommandConstants.COMMAND);
                if (rawCommandName != null && rawCommandName instanceof String) {
                    commandNames.add((String) rawCommandName);
                }
            }
            ctx.out().println("Available commands:");
            commandNames.stream().sorted().forEach(ctx.out()::println);
        } else {
            final Collection<ServiceReference<Command>> refs =
                    bundleContext.getServiceReferences(Command.class, "(" + CommandConstants.COMMAND + "=" + cmdName + ")");
            if (refs.isEmpty()) {
                ctx.err().println("Command not found: " + cmdName);
            } else {
                if (refs.size() > 1) {
                    ctx.err().println("More than one command found: showing first");
                }
                final ServiceReference<Command> ref = refs.iterator().next();
                try {
                    final Command cmd = bundleContext.getService(ref);
                    cmd.help(ctx.out());
                } finally {
                    bundleContext.ungetService(ref);
                }
            }
        }
    }
}

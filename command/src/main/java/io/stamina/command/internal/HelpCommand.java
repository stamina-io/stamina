/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.command.internal;

import io.stamina.command.Command;
import io.stamina.command.CommandConstants;
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
 * @author Stamina developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND_PROPERTY + "=help")
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
                final Object rawCommandName = ref.getProperty(CommandConstants.COMMAND_PROPERTY);
                if (rawCommandName != null && rawCommandName instanceof String) {
                    commandNames.add((String) rawCommandName);
                }
            }
            ctx.out().println("Available commands:");
            commandNames.stream().sorted().forEach(ctx.out()::println);
        } else {
            final Collection<ServiceReference<Command>> refs =
                    bundleContext.getServiceReferences(Command.class, "(" + CommandConstants.COMMAND_PROPERTY + "=" + cmdName + ")");
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

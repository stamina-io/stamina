/*
 * Copyright (c) 2017 Stamina developers.
 * All rights reserved.
 */

package io.stamina.command.internal;

import io.stamina.command.Command;
import io.stamina.command.CommandConstants;
import org.osgi.service.component.annotations.Component;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Simple command which outputs 'Hello %name!'.
 *
 * @author Stamina developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND_PROPERTY + "=hello")
public class HelloCommand implements Command {
    @Override
    public void execute(String[] arguments, InputStream in, PrintStream out, PrintStream err) throws Exception {
        final String name = arguments.length == 0 ? "world" : String.join(" ", arguments);
        out.println("Hello " + name + "!");
    }

    @Override
    public void help(PrintStream out) {
        out.println("Show some greetings.");
        out.println("Usage: hello [<name>]");
    }
}

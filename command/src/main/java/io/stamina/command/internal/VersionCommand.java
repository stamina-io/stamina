/*
 * Copyright (c) 2017 Stamina developers.
 * All rights reserved.
 */

package io.stamina.command.internal;

import io.stamina.command.Command;
import io.stamina.command.CommandConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Command displaying platform version.
 *
 * @author Stamina developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND_PROPERTY + "=version")
public class VersionCommand implements Command {
    private String version;

    @Activate
    public void activate(BundleContext bundleContext) {
        version = bundleContext.getBundle().getVersion().toString();
    }

    @Override
    public void execute(String[] arguments, InputStream in, PrintStream out, PrintStream err) throws Exception {
        out.println(version);
    }

    @Override
    public void help(PrintStream out) {
        out.println("Show platform version.");
        out.println("Usage: version");
    }
}

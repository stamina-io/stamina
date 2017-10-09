/*
 * Copyright (c) 2017 Stamina.io developers.
 * All rights reserved.
 */

package io.stamina.it;

import io.stamina.boot.helper.CommandLine;
import io.stamina.command.Command;
import io.stamina.command.CommandConstants;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import static io.stamina.it.TestHelper.lookupBundle;
import static io.stamina.it.TestHelper.options;
import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * Integration tests for bundle <code>io.stamina.command</code>.
 *
 * @author Stamina.io developers
 */
@RunWith(PaxExam.class)
public class CommandTest {
    @Inject
    private BundleContext bundleContext;
    private boolean executed;

    @Configuration
    public Option[] config() {
        return options(
                mavenBundle("io.stamina", "io.stamina.boot.helper").versionAsInProject().noStart(),
                mavenBundle("io.stamina", "io.stamina.command").versionAsInProject()
        );
    }

    @Test
    public void testExecuteNoCommand() throws BundleException {
        final File f = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getDataFile("exec");
        assertFalse(f.exists());

        final Command cmd = new Command() {
            @Override
            public void help(PrintStream out) {
            }

            @Override
            public void execute(Context ctx) throws Exception {
                f.createNewFile();
            }
        };
        final Dictionary<String, Object> cmdProps = new Hashtable<>(1);
        cmdProps.put(CommandConstants.COMMAND_PROPERTY, "test");
        bundleContext.registerService(Command.class, cmd, cmdProps);
        lookupBundle(bundleContext, "io.stamina.boot.helper").start();
        assertFalse(f.exists());
    }

    @Test
    @Ignore("This test does not run successfully yet")
    public void testExecuteCommand() throws BundleException, InterruptedException, IOException {
        final CommandLine cmdLine = new CommandLine() {
            @Override
            public String command() {
                return "test";
            }

            @Override
            public String[] arguments() {
                return new String[]{"123"};
            }

            @Override
            public String workingDirectory() {
                return "foo";
            }
        };
        bundleContext.registerService(CommandLine.class, cmdLine, null);

        final Path p = Files.createTempFile(null, null);
        assertEquals(0, p.toFile().length());

        final Command cmd = new Command() {
            @Override
            public void help(PrintStream out) {
            }

            @Override
            public void execute(Context ctx) throws Exception {
                if (ctx.arguments().length == 1 && "123".equals(ctx.arguments()[0])) {
                    Files.write(p, Collections.singletonList("executed"));
                    Thread.sleep(250);
                }
            }
        };
        final Dictionary<String, Object> cmdProps = new Hashtable<>(1);
        cmdProps.put(CommandConstants.COMMAND_PROPERTY, "test");
        bundleContext.registerService(Command.class, cmd, cmdProps);
        assertNotEquals(0, p.toFile().length());
    }
}

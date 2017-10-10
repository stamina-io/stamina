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

import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.stamina.starter.it.StaminaOptions.staminaDistribution;
import static java.lang.Thread.sleep;
import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.options;

/**
 * Integration tests for bundle <code>io.stamina.command</code>.
 *
 * @author Stamina.io developers
 */
@RunWith(PaxExam.class)
public class CommandTest {
    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        return options(
                staminaDistribution()
        );
    }

    @Test
    public void testExecuteNoCommandLine() throws BundleException, InterruptedException {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final Command cmd = new Command() {
            @Override
            public void help(PrintStream out) {
            }

            @Override
            public void execute(Context ctx) throws Exception {
                executed.set(true);
            }
        };
        final Dictionary<String, Object> cmdProps = new Hashtable<>(1);
        cmdProps.put(CommandConstants.COMMAND_PROPERTY, "test");
        bundleContext.registerService(Command.class, cmd, cmdProps);
        sleep(250);
        assertFalse(executed.get());
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
                    sleep(250);
                }
            }
        };
        final Dictionary<String, Object> cmdProps = new Hashtable<>(1);
        cmdProps.put(CommandConstants.COMMAND_PROPERTY, "test");
        bundleContext.registerService(Command.class, cmd, cmdProps);
        assertNotEquals(0, p.toFile().length());
    }
}

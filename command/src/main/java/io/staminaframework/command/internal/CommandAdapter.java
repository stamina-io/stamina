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
import org.apache.felix.gogo.runtime.CommandNotFoundException;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * This component adapts an OSGi command (RFC 147 compliant) to a Stamina command.
 * This enables to run an OSGi command from the command-line.
 *
 * @author Stamina Framework developers
 */
@Component(service = Command.class, property = CommandConstants.COMMAND + "=exec")
public class CommandAdapter implements Command {
    @Reference
    private CommandProcessor commandProcessor;
    @Reference
    private LogService logService;

    @Override
    public void help(PrintStream out) {
        out.println("Execute an OSGi command.");
        out.println("Usage: exec <command> [<command arguments>]");
    }

    @Override
    public boolean execute(Context context) throws Exception {
        if (context.arguments().length < 1) {
            help(context.out());
            return false;
        }

        boolean executed = false;
        String commandNotFound = null;
        for (int i = 0; !executed && i < 5; ++i) {
            try (final CommandSession session = commandProcessor.createSession(System.in, System.out, System.err)) {
                final String cmdLine = String.join(" ", Arrays.asList(context.arguments()));

                logService.log(LogService.LOG_INFO, "Executing OSGi command: " + cmdLine);
                final Object result = session.execute(cmdLine);
                executed = true;
                if (result != null) {
                    context.out().println(session.format(result, Converter.INSPECT));
                }
            } catch (CommandNotFoundException e) {
                logService.log(LogService.LOG_DEBUG, "OSGi command not available: trying again");
                commandNotFound = e.getCommand();
                Thread.sleep(1000);
            }
        }
        if (!executed) {
            throw new IllegalArgumentException("Command not found: " + commandNotFound);
        }
        return false;
    }
}

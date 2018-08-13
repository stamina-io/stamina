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

package io.staminaframework.runtime.boot.internal;

import io.staminaframework.runtime.boot.CommandLine;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Component responsible for publishing a {@link CommandLine}
 * instance to the Service Registry, if a command was invoked.
 *
 * @author Stamina Framework developers
 */
@Component
public class CommandLineManager {
    @Reference(service = LoggerFactory.class)
    private Logger logger;

    @Activate
    public void activate(BundleContext bundleContext) {
        final File cmdFile = bundleContext.getDataFile("cmd.dat");
        if (cmdFile.exists()) {
            logger.debug("Reading command-line data file: {}", cmdFile);

            try (final DataInputStream in = new DataInputStream(new FileInputStream(cmdFile))) {
                final String cmd = in.readUTF();
                if (cmd == null) {
                    throw new IOException("Invalid command-line data file: command is null");
                }
                final int cmdArgsLen = in.readInt();
                if (cmdArgsLen < 0) {
                    throw new IOException("Invalid command-line data file: incorrect number of arguments");
                }
                final String[] cmdArgs = new String[cmdArgsLen];
                for (int i = 0; i < cmdArgsLen; ++i) {
                    cmdArgs[i] = in.readUTF();
                    if (cmdArgs[i] == null) {
                        throw new IOException("Invalid command-line data file: found null argument");
                    }
                }

                final String wd = System.getProperty("user.dir");
                final CommandLineImpl cmdLine = new CommandLineImpl(wd, cmd, cmdArgs);
                final Dictionary<String, Object> cmdLineProps = new Hashtable<>(1);
                cmdLineProps.put("command", cmd);

                logger.info("Command-line found: $ {}", cmdLine);
                bundleContext.registerService(CommandLine.class, cmdLine, cmdLineProps);
            } catch (IOException e) {
                logger.error("Failed to read command-line data", e);
            } finally {
                // It's safe to remove the command-line data file once it's been read.
                cmdFile.delete();
            }
        } else {
            logger.debug("No command-line data file found");
        }
    }
}

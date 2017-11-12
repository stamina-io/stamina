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

package io.staminaframework.runtime.launcher;

import org.apache.felix.utils.properties.Properties;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Configuration utilities.
 *
 * @author Stamina Framework developers
 */
final class ConfigurationUtils {
    private ConfigurationUtils() {
    }

    /**
     * Load configuration file.
     *
     * @param file    file to read
     * @param context framework bundle context
     * @return configuration properties
     */
    public static Map<String, String> loadConfiguration(Path file, BundleContext context) throws IOException {
        if (!Files.exists(file)) {
            return new HashMap<>(4);
        }

        final Properties props = new Properties(file.toFile(), context);
        final String includeProp = props.getProperty("${includes}");
        if (includeProp != null) {
            final StringTokenizer tokens = new StringTokenizer(includeProp, " ");
            while (tokens.hasMoreElements()) {
                final String filePathToInclude = tokens.nextToken().trim();
                if (filePathToInclude.length() == 0) {
                    continue;
                }
                final Path fileToInclude = file.getParent().resolve(filePathToInclude);
                if (Files.exists(fileToInclude)) {
                    final Map<String, String> propsToInclude = loadConfiguration(fileToInclude, context);
                    if (propsToInclude != null && !propsToInclude.isEmpty()) {
                        props.putAll(propsToInclude);
                    }
                }
            }
        }

        return props;
    }
}

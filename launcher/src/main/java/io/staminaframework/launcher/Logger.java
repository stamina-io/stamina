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

package io.staminaframework.launcher;

import java.util.function.Supplier;

/**
 * Application logger.
 *
 * @author Stamina Framework developers
 */
public interface Logger {
    /**
     * Log an entry at DEBUG level.
     */
    void debug(Supplier<String> msg);

    /**
     * Log an entry at INFO level.
     */
    void info(Supplier<String> msg);

    /**
     * Log an entry at WARN level.
     */
    void warn(Supplier<String> msg);

    /**
     * Log an entry at FATAL level, and then halt current process.
     */
    void fatal(Supplier<String> msg, Throwable cause);
}

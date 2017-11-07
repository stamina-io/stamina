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

package io.staminaframework.addon;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Service interface for a component managing addons.
 *
 * @author Stamina Framework developers
 */
@ConsumerType
public interface AddonAdmin {
    /**
     * Install an addon.
     * If an addon is already installed, this method does nothing.
     *
     * @param location addon location
     * @throws RuntimeException if addon installation failed
     */
    void install(String location);
}

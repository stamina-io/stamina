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

package io.staminaframework.addon.internal;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class AddonRequirement implements Requirement {
    private final Map<String, String> directives = new HashMap<>(1);

    public AddonRequirement(final String symbolicName, final String version) {
        final String filter;
        if (version == null) {
            filter = "(&(osgi.identity=" + symbolicName + ")(type=osgi.subsystem.feature))";
        } else {
            filter = "(&(osgi.identity=" + symbolicName + ")(type=osgi.subsystem.feature)(version=" + version + "))";
        }
        directives.put("filter", filter);
    }

    @Override
    public String getNamespace() {
        return "osgi.identity";
    }

    @Override
    public Map<String, String> getDirectives() {
        return directives;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public Resource getResource() {
        return null;
    }
}

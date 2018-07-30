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

package io.staminaframework.runtime.starter.it;

import org.apache.felix.framework.util.FelixConstants;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Constants;

import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * Helper class providing Pax-Exam {@link org.ops4j.pax.exam.Option} instances
 * for handling a Stamina platform.
 *
 * @author Stamina Framework developers
 */
public final class StaminaOptions {
    private StaminaOptions() {
    }

    /**
     * Configure a Stamina-like OSGi distribution, with the same bundles.
     *
     * @return Pax-Exam option for creating a Stamina-like distribution
     */
    public static Option staminaDistribution() {
        return CoreOptions.composite(
                mavenBundle("org.apache.felix", "org.apache.felix.log.extension").versionAsInProject().startLevel(1),
                mavenBundle("io.staminaframework.runtime", "io.staminaframework.runtime.starter.it").versionAsInProject(),
                mavenBundle("io.staminaframework.runtime", "io.staminaframework.runtime.boot").versionAsInProject(),
                mavenBundle("io.staminaframework.runtime", "io.staminaframework.runtime.command").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.configurator").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.converter").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.fileinstall").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.eventadmin").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.scr").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.resolver").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.bundlerepository").versionAsInProject(),
                mavenBundle("org.osgi", "osgi.promise").versionAsInProject(),
                mavenBundle("org.osgi", "org.osgi.util.function").versionAsInProject(),
                mavenBundle("org.osgi", "org.osgi.util.pushstream").versionAsInProject(),
                mavenBundle("org.apache.aries.async", "org.apache.aries.async").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.coordinator").versionAsInProject(),
                mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
                mavenBundle("org.eclipse.equinox", "org.eclipse.equinox.region").versionAsInProject(),
                mavenBundle("org.apache.aries.subsystem", "org.apache.aries.subsystem").versionAsInProject(),
                mavenBundle("io.staminaframework.runtime", "io.staminaframework.runtime.subsystem").versionAsInProject(),
                mavenBundle("io.staminaframework.runtime", "io.staminaframework.runtime.addon").versionAsInProject(),
                frameworkProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL).value("100"),
                frameworkProperty(FelixConstants.BUNDLE_STARTLEVEL_PROP).value("80"),
                frameworkProperty(Constants.FRAMEWORK_STORAGE_CLEAN).value(Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT),
                systemPackage("org.slf4j;version=1.7"),
                systemPackage("org.osgi.service.log;version=1.4"),
                systemPackage("org.osgi.service.log.admin;version=1.0"),
                junitBundles()
        );
    }
}

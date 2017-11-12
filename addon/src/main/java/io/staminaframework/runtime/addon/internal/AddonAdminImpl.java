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

package io.staminaframework.runtime.addon.internal;

import io.staminaframework.runtime.addon.AddonAdmin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

/**
 * This component installs addons on startup.
 *
 * @author Stamina Framework developers
 */
@Component(service = AddonAdmin.class, immediate = true,
        configurationPid = "io.staminaframework.runtime.addon")
public class AddonAdminImpl implements AddonAdmin {
    @Reference
    private LogService logService;
    @Reference(target = "(" + SubsystemConstants.SUBSYSTEM_ID_PROPERTY + "=0)")
    private Subsystem root;

    // Force URL handler "addon" initialization.
    @Reference(target = "(" + URLConstants.URL_HANDLER_PROTOCOL + "=addon)")
    private URLStreamHandlerService addonUrlHandler;

    private BundleContext bundleContext;

    /**
     * Component configuration.
     *
     * @author Stamina Framework developers
     */
    public @interface Config {
        /**
         * Addons to install on startup.
         */
        String[] addons() default {};
    }

    @Activate
    void activate(BundleContext bundleContext, Config config) {
        this.bundleContext = bundleContext;
        if (config.addons() != null) {
            for (final String addon : config.addons()) {
                final String addonTrimmed = addon.trim();
                if (addonTrimmed.length() != 0) {
                    install(addonTrimmed);
                }
            }
        }
    }

    @Deactivate
    void deactivate() {
        this.bundleContext = null;
    }

    @Override
    public void install(String location) {
        try {
            doInstallAddon(location);
        } catch (Exception e) {
            throw new RuntimeException("Failed to install addon: " + location, e);
        }
    }

    private void doInstallAddon(String location) {
        if (isSubsystemInstalled(location, root)) {
            logService.log(LogService.LOG_DEBUG, "Addon is already installed: " + location);
            return;
        }

        logService.log(LogService.LOG_INFO, "Installing addon: " + location);
        final Subsystem subsystem = root.install(location);

        logService.log(LogService.LOG_INFO, "Starting addon: " + location);
        subsystem.start();

        // Refresh bundles, in case the new subsystem brings major package updates.
        final Bundle sysBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        sysBundle.adapt(FrameworkWiring.class).refreshBundles(null);
    }

    private boolean isSubsystemInstalled(String location, Subsystem parent) {
        if (location.equalsIgnoreCase(parent.getLocation())) {
            return true;
        }
        for (final Subsystem child : parent.getChildren()) {
            if (isSubsystemInstalled(location, child)) {
                return true;
            }
        }
        return false;
    }
}

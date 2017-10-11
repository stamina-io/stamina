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

package io.staminaframework.starter.it;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;

/**
 * Assertions utilities.
 *
 * @author Stamina Framework developers
 */
public final class Assert {
    private Assert() {
    }

    /**
     * Check if a bundle state is correct.
     *
     * @param ctx          bundle context injected by Pax-Exam
     * @param symbolicName bundle symbolic name to check
     * @param bundleState  bundle state to check
     */
    public static void assertBundleState(BundleContext ctx, String symbolicName, int bundleState) {
        final Bundle b = OsgiHelper.lookupBundle(ctx, symbolicName);
        assertEquals("Invalid bundle state: " + symbolicName + "=" + b.getState(),
                bundleState, b.getState());
    }
}

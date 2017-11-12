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

package io.staminaframework.runtime.provisioning.internal;

final class RepoUtils {
    private RepoUtils() {
    }

    public static boolean sameObrUrl(String obrUrl, String felixObrUrl) {
        // Apache Felix BundleRepository is doing weird things with OBR URLs:
        // the registered URL is not the same which is returned when listing
        // repositories.
        // This method will try to compare a full OBR URL with Felix one.

        String newUrl = obrUrl;
        if (obrUrl.endsWith("zip")) {
            newUrl = "jar:".concat(obrUrl).concat("!/");
        } else if (obrUrl.endsWith(".xml")) {
            newUrl = obrUrl.substring(0, obrUrl.lastIndexOf('/') + 1);
        }
        return newUrl.equals(felixObrUrl);
    }
}

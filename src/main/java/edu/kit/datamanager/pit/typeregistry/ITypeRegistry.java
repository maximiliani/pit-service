/*
 * Copyright (c) 2025 Karlsruhe Institute of Technology.
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

package edu.kit.datamanager.pit.typeregistry;

import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.concurrent.CompletableFuture;

/**
 * Main abstraction interface towards the type registry. Contains all methods
 * required from the registry by the core services.
 */
@Observed
public interface ITypeRegistry {
    @WithSpan
    @Timed
    CompletableFuture<AttributeInfo> queryAttributeInfo(String attributePid);

    @WithSpan
    @Timed
    CompletableFuture<RegisteredProfile> queryAsProfile(String profilePid);

    /**
     * An identifier for exceptions and debugging purposes.
     *
     * @return a name ur url string that identifies the implementation or configuration well in case of errors.
     */
    String getRegistryIdentifier();
}

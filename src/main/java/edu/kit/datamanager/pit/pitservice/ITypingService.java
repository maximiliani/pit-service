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

package edu.kit.datamanager.pit.pitservice;

import edu.kit.datamanager.pit.common.ExternalServiceException;
import edu.kit.datamanager.pit.common.RecordValidationException;
import edu.kit.datamanager.pit.domain.Operations;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * Core interface for clients to contact. Implementations will provide the
 * necessary fixation in a concrete protocol (e.g. HTTP-REST).
 */
@Observed
public interface ITypingService extends IIdentifierSystem {

    void setValidationStrategy(IValidationStrategy strategy);

    @Timed
    @WithSpan
    void validate(PIDRecord pidRecord)
            throws RecordValidationException, ExternalServiceException;

    /**
     * Returns an operations instance, configured with this typingService.
     * <p>
     * Convenience method for `new Operations(typingService)`.
     *
     * @return an operation instance.
     */
    Operations getOperations();
}

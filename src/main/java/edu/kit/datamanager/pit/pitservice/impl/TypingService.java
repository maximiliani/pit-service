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

package edu.kit.datamanager.pit.pitservice.impl;

import edu.kit.datamanager.pit.common.*;
import edu.kit.datamanager.pit.domain.Operations;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import edu.kit.datamanager.pit.pitservice.ITypingService;
import edu.kit.datamanager.pit.pitservice.IValidationStrategy;
import edu.kit.datamanager.pit.typeregistry.AttributeInfo;
import edu.kit.datamanager.pit.typeregistry.ITypeRegistry;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

/**
 * Core implementation class that offers the combined higher-level services
 * through a type registry and an identifier system.
 */
@Observed
public class TypingService implements ITypingService {

    private static final Logger LOG = LoggerFactory.getLogger(TypingService.class);
    private static final String LOG_MSG_TYPING_SERVICE_MISCONFIGURED = "Typing service misconfigured.";

    protected final IIdentifierSystem identifierSystem;
    protected final ITypeRegistry typeRegistry;

    /**
     * A validation strategy. Will never be null.
     * <p>
     * ApplicationProperties::defaultValidationStrategy there is always either a
     * default strategy or a noop strategy assigned. Therefore, autowiring will
     * always work. Assigning null is done to avoid warnings on constructor.
     */
    protected IValidationStrategy defaultStrategy;

    public TypingService(IIdentifierSystem identifierSystem, ITypeRegistry typeRegistry, IValidationStrategy defaultStrategy) {
        super();
        this.identifierSystem = identifierSystem;
        this.typeRegistry = typeRegistry;
        this.defaultStrategy = defaultStrategy;
    }

    @Override
    public Optional<String> getPrefix() {
        return this.identifierSystem.getPrefix();
    }

    @Override
    public void setValidationStrategy(IValidationStrategy strategy) {
        this.defaultStrategy = strategy;
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public void validate(@SpanAttribute PIDRecord pidRecord)
            throws RecordValidationException, ExternalServiceException {
        this.defaultStrategy.validate(pidRecord);
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public boolean isPidRegistered(@SpanAttribute String pid) throws ExternalServiceException {
        LOG.trace("Performing isIdentifierRegistered({}).", pid);
        return identifierSystem.isPidRegistered(pid);
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public String registerPidUnchecked(@SpanAttribute final PIDRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException {
        LOG.trace("Performing registerPID({}).", pidRecord);
        return identifierSystem.registerPidUnchecked(pidRecord);
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public boolean deletePid(@SpanAttribute String pid) throws ExternalServiceException {
        LOG.trace("Performing deletePID({}).", pid);
        return identifierSystem.deletePid(pid);
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public PIDRecord queryPid(@SpanAttribute String pid) throws PidNotFoundException, ExternalServiceException {
        return queryPid(pid, false);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public PIDRecord queryPid(@SpanAttribute String pid, @SpanAttribute boolean includePropertyNames)
            throws PidNotFoundException, ExternalServiceException {
        LOG.trace("Performing queryAllProperties({}, {}).", pid, includePropertyNames);
        PIDRecord pidInfo = identifierSystem.queryPid(pid);

        if (includePropertyNames) {
            enrichPIDInformationRecord(pidInfo);
        }
        return pidInfo;
    }

    private void enrichPIDInformationRecord(PIDRecord pidInfo) {
        // enrich record by querying type registry for all property definitions
        // to get the property names
        for (String typeIdentifier : pidInfo.getPropertyIdentifiers()) {
            AttributeInfo attributeInfo;
            try {
                attributeInfo = this.typeRegistry.queryAttributeInfo(typeIdentifier).join();
            } catch (CompletionException | CancellationException ex) {
                // TODO convert exceptions like in validation service.
                throw new InvalidConfigException(LOG_MSG_TYPING_SERVICE_MISCONFIGURED);
            }

            if (attributeInfo != null) {
                pidInfo.setPropertyName(typeIdentifier, attributeInfo.name());
            } else {
                pidInfo.setPropertyName(typeIdentifier, typeIdentifier);
            }
        }
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public boolean updatePid(@SpanAttribute PIDRecord pidRecord) throws PidNotFoundException, ExternalServiceException, RecordValidationException {
        return this.identifierSystem.updatePid(pidRecord);
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public Collection<String> resolveAllPidsOfPrefix() throws ExternalServiceException, InvalidConfigException {
        return this.identifierSystem.resolveAllPidsOfPrefix();
    }

    @Override
    @WithSpan
    @Timed
    public Operations getOperations() {
        return new Operations(this.typeRegistry, this.identifierSystem);
    }

}

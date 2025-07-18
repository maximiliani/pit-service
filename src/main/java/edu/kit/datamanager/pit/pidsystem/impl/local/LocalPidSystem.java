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

package edu.kit.datamanager.pit.pidsystem.impl.local;

import edu.kit.datamanager.pit.common.*;
import edu.kit.datamanager.pit.configuration.ApplicationProperties;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.pidsystem.IIdentifierSystem;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A system that stores PIDs on the local machine, in its configured database.
 * <p>
 * Purpose: This local system is made for demonstrations, preparations or local
 * tests, but may also be used for other cases where PIDs should not be public
 * (yet).
 * <p>
 * Note: This system has its own PID string format and we can not guarantee that
 * you'll be able to register your PIDs later in another system with the same
 * format. If you need this feature, feel free to open an issue on GitHub:
 * <a href="https://github.com/kit-data-manager/pit-service">https://github.com/kit-data-manager/pit-service</a>
 * <p>
 * Configuration: The database configuration of this service is done via the
 * `spring.datasource.*` properties. There is no configuration that controls a
 * separate database only for this system. Consider the InMemoryIdentifierSystem
 * for this.
 */
@Component
@AutoConfigureAfter(value = ApplicationProperties.class)
@ConditionalOnExpression(
        "#{ '${pit.pidsystem.implementation}' eq T(edu.kit.datamanager.pit.configuration.ApplicationProperties.IdentifierSystemImpl).LOCAL.name() }"
)
@Transactional
@Observed
public class LocalPidSystem implements IIdentifierSystem {

    private static final Logger LOG = LoggerFactory.getLogger(LocalPidSystem.class);
    private static final String PREFIX = "sandboxed/";
    @Autowired
    private PidDatabaseObjectDao db;

    public LocalPidSystem() {
        LOG.warn("Using local identifier system to store PIDs. REGISTERED PIDs ARE NOT PERMANENTLY OR PUBLICLY STORED.");
    }

    /**
     * For testing purposes.
     */
    protected PidDatabaseObjectDao getDatabase() {
        return this.db;
    }

    /**
     * For testing only. Allows to inject the database access object afterwards.
     *
     * @param db the new DAO.
     */
    protected void setDatabase(PidDatabaseObjectDao db) {
        this.db = db;
    }

    @Override
    public Optional<String> getPrefix() {
        return Optional.of(PREFIX);
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public boolean isPidRegistered(@SpanAttribute String pid) throws ExternalServiceException {
        return this.db.existsById(pid);
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public PIDRecord queryPid(@SpanAttribute String pid) throws PidNotFoundException, ExternalServiceException {
        Optional<PidDatabaseObject> dbo = this.db.findByPid(pid);
        return new PIDRecord(dbo.orElseThrow(() -> new PidNotFoundException(pid)));
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public String registerPidUnchecked(@SpanAttribute final PIDRecord pidRecord) throws PidAlreadyExistsException, ExternalServiceException {
        if (this.db.existsById(pidRecord.getPid())) {
            throw new PidAlreadyExistsException(pidRecord.getPid());
        }
        this.db.save(new PidDatabaseObject(pidRecord));
        LOG.debug("Registered record with PID: {}", pidRecord.getPid());
        return pidRecord.getPid();
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public boolean updatePid(@SpanAttribute PIDRecord rec) throws PidNotFoundException, ExternalServiceException, RecordValidationException {
        if (this.db.existsById(rec.getPid())) {
            this.db.save(new PidDatabaseObject(rec));
            return true;
        }
        return false;
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public boolean deletePid(@SpanAttribute String pid) {
        throw new UnsupportedOperationException("Deleting PIDs is against the P in PID.");
    }

    @Override
    @WithSpan(kind = SpanKind.CLIENT)
    @Timed
    @Counted
    public Collection<String> resolveAllPidsOfPrefix() throws ExternalServiceException, InvalidConfigException {
        return this.db.findAll().parallelStream()
                .map(PidDatabaseObject::getPid)
                .filter(pid -> pid.startsWith(PREFIX))
                .collect(Collectors.toSet());
    }
}

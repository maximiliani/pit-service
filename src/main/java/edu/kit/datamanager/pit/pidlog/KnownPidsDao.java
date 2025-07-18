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

package edu.kit.datamanager.pit.pidlog;

import io.micrometer.observation.annotation.Observed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * Object to access known PIDs from the database.
 * <p>
 * Method implementation documentation is skipped due to automated
 * implementation via spring data, documented in
 * <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods">JPA query methods</a>
 * <p>
 * as well as the general concept documented in
 * <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.core-concepts">JPA repository core concepts</a>
 */
@Observed
public interface KnownPidsDao extends JpaRepository<KnownPid, String>, JpaSpecificationExecutor<KnownPid> {
    Optional<KnownPid> findByPid(String pid);

    Collection<KnownPid> findDistinctPidsByCreated(Instant created);

    Collection<KnownPid> findDistinctPidsByModified(Instant modified);

    Collection<KnownPid> findDistinctPidsByCreatedBetween(Instant from, Instant to);

    Collection<KnownPid> findDistinctPidsByModifiedBetween(Instant from, Instant to);

    Page<KnownPid> findDistinctPidsByCreated(Instant created, Pageable pageable);

    Page<KnownPid> findDistinctPidsByModified(Instant modified, Pageable pageable);

    Page<KnownPid> findDistinctPidsByCreatedBetween(Instant from, Instant to, Pageable pageable);

    Page<KnownPid> findDistinctPidsByModifiedBetween(Instant from, Instant to, Pageable pageable);

    long countDistinctPidsByCreatedBetween(Instant from, Instant to);

    long countDistinctPidsByModifiedBetween(Instant from, Instant to);
}

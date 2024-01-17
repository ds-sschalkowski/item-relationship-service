/********************************************************************************
 * Copyright (c) 2021,2022,2023
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 *       2022,2023: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022,2023: BOSCH AG
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.irs.ess.service;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Incident Bpn Job Cache
 */
interface BpnInvestigationJobCache {

    List<BpnInvestigationJob> findAll();
    Optional<BpnInvestigationJob> findByJobId(UUID jobId);
    BpnInvestigationJob store(UUID jobId, BpnInvestigationJob bpnInvestigationJob);

}

/**
 * Temporary in memory implementation
 */
@Service
class InMemoryBpnInvestigationJobCache implements BpnInvestigationJobCache {

    private final ConcurrentHashMap<UUID, BpnInvestigationJob> inMemory = new ConcurrentHashMap<>();

    @Override
    public List<BpnInvestigationJob> findAll() {
        return new ArrayList<>(inMemory.values());
    }

    @Override
    public Optional<BpnInvestigationJob> findByJobId(final UUID jobId) {
        return Optional.ofNullable(inMemory.get(jobId));
    }

    @Override
    public BpnInvestigationJob store(final UUID jobId, final BpnInvestigationJob bpnInvestigationJob) {
        requireNonNull(bpnInvestigationJob);
        return inMemory.put(jobId, bpnInvestigationJob);
    }

}

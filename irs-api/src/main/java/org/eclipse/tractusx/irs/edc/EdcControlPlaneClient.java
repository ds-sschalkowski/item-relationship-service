/********************************************************************************
 * Copyright (c) 2021,2022
 *       2022: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0. *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.irs.edc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.tractusx.irs.configuration.EdcConfiguration;
import org.eclipse.tractusx.irs.edc.model.NegotiationId;
import org.eclipse.tractusx.irs.edc.model.NegotiationRequest;
import org.eclipse.tractusx.irs.edc.model.NegotiationResponse;
import org.eclipse.tractusx.irs.edc.model.TransferProcessId;
import org.eclipse.tractusx.irs.edc.model.TransferProcessRequest;
import org.eclipse.tractusx.irs.edc.model.TransferProcessResponse;
import org.eclipse.tractusx.irs.services.AsyncPollingService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Communicates with the EDC ControlPlane
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdcControlPlaneClient {

    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    private final RestTemplate simpleRestTemplate;
    private final AsyncPollingService pollingService;

    private final EdcConfiguration config;

    /* package */ Catalog getCatalog(final String providerConnectorUrl) {
        final var catalogUrl =
                config.getControlPlaneEndpointData() + "/catalog?providerUrl={providerUrl}&limit={limit}";
        final var providerUrl = providerConnectorUrl + config.getControlPlaneProviderSuffix();
        final var limit = config.getControlPlaneCatalogLimit();

        return simpleRestTemplate.exchange(catalogUrl, HttpMethod.GET, new HttpEntity<>(null, headers()), Catalog.class,
                providerUrl, limit).getBody();
    }

    /* package */ NegotiationId startNegotiations(final NegotiationRequest request) {
        return simpleRestTemplate.exchange(config.getControlPlaneEndpointData() + "/contractnegotiations",
                HttpMethod.POST, new HttpEntity<>(request, headers()), NegotiationId.class).getBody();
    }

    /* package */ CompletableFuture<NegotiationResponse> getNegotiationResult(final NegotiationId negotiationId) {
        final HttpEntity<Object> objectHttpEntity = new HttpEntity<>(null, headers());

        return pollingService.<NegotiationResponse>createJob()
                             .action(() -> {
                                 log.info("Check negotiations status");

                                 final NegotiationResponse response = simpleRestTemplate.exchange(
                                         config.getControlPlaneEndpointData() + "/contractnegotiations/"
                                                 + negotiationId.getValue(), HttpMethod.GET, objectHttpEntity,
                                         NegotiationResponse.class).getBody();

                                 log.info("Response status of negotiation: {}", response);

                                 if (response != null && STATUS_CONFIRMED.equals(response.getState())) {
                                     return Optional.of(response);
                                 }
                                 return Optional.empty();
                             })
                             .description("wait for negotiation confirmation")
                             .timeToLive(config.getControlPlaneRequestTtl())
                             .build()
                             .schedule();

    }

    /* package */ TransferProcessId startTransferProcess(final TransferProcessRequest request) {
        return simpleRestTemplate.exchange(config.getControlPlaneEndpointData() + "/transferprocess", HttpMethod.POST,
                new HttpEntity<>(request, headers()), TransferProcessId.class).getBody();
    }

    /* package */ CompletableFuture<TransferProcessResponse> getTransferProcess(
            final TransferProcessId transferProcessId) {

        final HttpEntity<Object> objectHttpEntity = new HttpEntity<>(null, headers());

        return pollingService.<TransferProcessResponse>createJob()
                             .action(() -> {
                                 log.info("Check Transfer Process status");

                                 final TransferProcessResponse response = simpleRestTemplate.exchange(
                                         config.getControlPlaneEndpointData() + "/transferprocess/"
                                                 + transferProcessId.getValue(), HttpMethod.GET, objectHttpEntity,
                                         TransferProcessResponse.class).getBody();

                                 log.info("Response status of Transfer Process: {}", response);

                                 if (response != null && STATUS_COMPLETED.equals(response.getState())) {
                                     return Optional.of(response);
                                 }
                                 return Optional.empty();

                             })
                             .description("wait for transfer process completion")
                             .timeToLive(config.getControlPlaneRequestTtl())
                             .build()
                             .schedule();

    }

    private HttpHeaders headers() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.add(config.getControlPlaneApiKeyHeader(), config.getControlPlaneApiKeySecret());
        return headers;
    }

}

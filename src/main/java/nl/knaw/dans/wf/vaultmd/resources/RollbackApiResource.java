/*
 * Copyright (C) 2021 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.wf.vaultmd.resources;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
public class RollbackApiResource implements RollbackApi {
    private final Executor executor;

    @Override
    public Response rollbackPost(Object body) {
        log.info("Received rollback request: {}", body);
        log.warn("NOT IMPLEMENTED, IGNORING...");
        return Response.status(200).build();
    }
}

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
import nl.knaw.dans.wf.vaultmd.api.StepInvocationDto;
import nl.knaw.dans.wf.vaultmd.core.DataverseService;
import nl.knaw.dans.wf.vaultmd.core.IdMintingService;
import nl.knaw.dans.wf.vaultmd.core.IdValidator;
import nl.knaw.dans.wf.vaultmd.core.SetVaultMetadataTask;

import javax.ws.rs.core.Response;
import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
public class InvokeApiResource implements InvokeApi {
    private final Executor executor;
    private final DataverseService dataverseService;

    private final IdMintingService idMintingService;
    private final IdValidator idValidator;

    @Override
    public Response invokePost(StepInvocationDto inv) {
        log.info("Received invocation: {}", inv);
        executor.execute(new SetVaultMetadataTask(inv, dataverseService, idMintingService, idValidator));
        log.info("Added new task to queue");
        return Response.status(200).build();
    }
}

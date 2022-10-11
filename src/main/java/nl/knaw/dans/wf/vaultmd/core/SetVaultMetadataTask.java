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
package nl.knaw.dans.wf.vaultmd.core;

import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.FieldList;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataBlock;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import nl.knaw.dans.lib.dataverse.model.workflow.ResumeMessage;
import nl.knaw.dans.wf.vaultmd.api.StepInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class SetVaultMetadataTask implements Runnable {

    public static final String DANS_NBN = "dansNbn";
    public static final String DANS_BAG_ID = "dansBagId";
    private static final Logger log = LoggerFactory.getLogger(SetVaultMetadataTask.class);
    private final StepInvocation stepInvocation;
    //    private final DataverseClient dataverseClient;

    private final DataverseService dataverseService;

    private final String nbnPrefix = "nl:ui:13-";

    public SetVaultMetadataTask(StepInvocation stepInvocation, DataverseClient dataverseClient, DataverseService dataverseService) {
        this.stepInvocation = stepInvocation;
        //        this.dataverseClient = dataverseClient;
        this.dataverseService = dataverseService;
        // TODO DI
    }

    @Override
    public String toString() {
        return "SetVaultMetadataTask{" + "invocationId='" + stepInvocation.getInvocationId() + "'}";
    }

    @Override
    public void run() {
        log.info("Running task " + this);
        //        new SetVaultMetadataTaskScala(
        //            WorkflowVariables.apply(stepInvocation.getInvocationId(), stepInvocation.getGlobalId(), stepInvocation.getDatasetId(), stepInvocation.getMajorVersion(), stepInvocation.getMinorVersion()),
        //            dataverseClient, "nl:ui:13-", 10, 1000).run();

        runTask();
    }

    void runTask() {
        try {
            // lock dataset before doing work
            dataverseService.lockDataset(stepInvocation, "Workflow");

            // update metadata
            var metadata = getVaultMetadata(stepInvocation);
            dataverseService.editMetadata(stepInvocation, metadata);

            // resume workflow
            resumeWorkflow(stepInvocation);
        }
        catch (IOException | DataverseException e) {
            try {
                dataverseService.resumeWorkflow(stepInvocation,
                    new ResumeMessage("Failure", e.getMessage(), "Publication failed: pre-publication workflow returned an error"));
            }
            catch (IOException | DataverseException ex) {
                log.error("Error resuming workflow with Failure status", ex);
            }
        }
    }

    Optional<String> getVaultMetadataFieldValue(DatasetVersion dataset, String fieldName) {
        return Optional.ofNullable(dataset.getMetadataBlocks().get("dansDataVaultMetadata"))
            .map(MetadataBlock::getFields)
            .map(fields -> fields.stream()
                .filter(field -> field.getTypeName().equals(fieldName))
                .findFirst())
            .flatMap(i -> i)
            .map(f -> (PrimitiveSingleValueField) f)
            .map(PrimitiveSingleValueField::getValue);
    }

    FieldList getVaultMetadata(StepInvocation stepInvocation) throws IOException, DataverseException {
        var dsv = dataverseService.getVersion(stepInvocation, ":draft")
            .orElseThrow(() -> new IllegalArgumentException("No draft version found"));

        var latestVersion = dataverseService.getLatestReleasedOrDeaccessionedVersion(stepInvocation);

        //        var dsv = dataset.getVersion(":draft").getData();
        //        var latestVersion = dataset.getAllVersions().getData().stream()
        //            .filter(d -> Set.of("RELEASED", "DEACCESSIONED").contains(d.getVersionState()))
        //            .max(versionComparator);

        var bagId = latestVersion.map(m -> getBagId(dsv, m))
            .orElseGet(() -> getBagId(dsv));

        var nbn = latestVersion.map(this::getNbn)
            .orElseGet(() -> getVaultMetadataFieldValue(dsv, DANS_NBN).orElseGet(this::mintUrnNbn));

        var version = String.format("%s.%s", stepInvocation.getMajorVersion(), stepInvocation.getMinorVersion());

        log.debug("Generating metadata with values dansDataversePid={}, dansDataversePidVersion={}, {}={}, {}={}",
                stepInvocation.getGlobalId(), version, DANS_BAG_ID, bagId, DANS_NBN, nbn);

        var fieldList = new FieldList();
        fieldList.add(new PrimitiveSingleValueField("dansDataversePid", stepInvocation.getGlobalId()));
        fieldList.add(new PrimitiveSingleValueField("dansDataversePidVersion", version));
        fieldList.add(new PrimitiveSingleValueField(DANS_BAG_ID, bagId));
        fieldList.add(new PrimitiveSingleValueField(DANS_NBN, nbn));

        return fieldList;
    }

    void resumeWorkflow(StepInvocation stepInvocation) throws IOException, DataverseException {
        dataverseService.resumeWorkflow(stepInvocation, new ResumeMessage("Success", "", ""));
        // TODO do the retry logic that scala has
        //        dataverseClient.workflows().resume(invocationId, new ResumeMessage("Success", "", ""));
    }

    /*
    private def resumeWorkflow(invocationId: String): Try[Unit] = {
        logger.trace(s"$maxNumberOfRetries $timeBetweenRetries")
        var numberOfTimesTried = 0
            var invocationIdNotFound = true

        do {
            val resumeResponse = Try { dataverse.workflows().resume(invocationId, new ResumeMessage("Success", "", "")) }
            invocationIdNotFound = checkForInvocationIdNotFoundError(resumeResponse, invocationId).unsafeGetOrThrow

            if (invocationIdNotFound) {
                logger.debug(s"Sleeping $timeBetweenRetries ms before next try..")
                sleep(timeBetweenRetries)
                numberOfTimesTried += 1
            }
        } while (numberOfTimesTried <= maxNumberOfRetries && invocationIdNotFound)

        if (invocationIdNotFound) {
            logger.error(s"Workflow could not be resumed for dataset ${ workFlowVariables.globalId }. Number of retries: $maxNumberOfRetries. Time between retries: $timeBetweenRetries")
            Failure(InvocationIdNotFoundException(maxNumberOfRetries, timeBetweenRetries))
        }
        else Success(())
    }

     */
    String getNbn(DatasetVersion latestPublishedDataset) {
        // validate latest published version has a bag id
        return getVaultMetadataFieldValue(latestPublishedDataset, DANS_NBN)
            .orElseThrow(() -> new IllegalArgumentException("Dataset with a latest published version without NBN found!"));
    }

    String getBagId(DatasetVersion draftVersion) {
        var draftBagId = getVaultMetadataFieldValue(draftVersion, DANS_BAG_ID);

        return getVaultMetadataFieldValue(draftVersion, DANS_BAG_ID)
            .orElseGet(() -> draftBagId.orElse(mintBagId()));
    }

    String getBagId(DatasetVersion draftVersion, DatasetVersion latestPublishedDataset) {
        var draftBagId = getVaultMetadataFieldValue(draftVersion, DANS_BAG_ID);

        /*
        create a new bag id if:
        - the draft bag doesn't have a bag id
        - the latest published bag id is the same as the draft bag id
        - the latest published version does not exist, and the bag id in the draft is also empty
         */
        return getVaultMetadataFieldValue(latestPublishedDataset, DANS_BAG_ID)
            .map(latestBagId -> {
                if (draftBagId.isEmpty() || latestBagId.equals(draftBagId.orElse(null))) {
                    /*
                     * This happens after publishing a new version via the UI. The bagId from the previous version is inherited by the new draft. However, we
                     * want every version to have a unique bagId.
                     */
                    return mintBagId();
                }
                else {
                    /*
                     * Provided by machine deposit.
                     */
                    return draftBagId.get();
                }
            }).orElseThrow(() -> new IllegalArgumentException("Dataset with a latest published version without bag ID found!"));
    }

    String mintUrnNbn() {
        return String.format("urn:nbn:%s%s", nbnPrefix, UUID.randomUUID());
    }

    String mintBagId() {
        return String.format("urn:uuid:%s", UUID.randomUUID());
    }

}

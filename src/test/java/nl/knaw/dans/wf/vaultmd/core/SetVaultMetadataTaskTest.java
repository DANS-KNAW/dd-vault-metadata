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

import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.FieldList;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataBlock;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import nl.knaw.dans.lib.dataverse.model.workflow.ResumeMessage;
import nl.knaw.dans.wf.vaultmd.api.StepInvocation;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

class SetVaultMetadataTaskTest {

    private final DataverseService dataverseService = Mockito.mock(DataverseService.class);

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(dataverseService);
    }

    @Test
    void run() throws IOException, DataverseException {
        var draft = createDatasetVersion("bagid", "nbn", 1, 1, "DRAFT");
        var previous = createDatasetVersion("bagid", "nbn", 1, 0, "RELEASED");

        Mockito.when(dataverseService.getVersion(Mockito.any(), Mockito.any()))
            .thenReturn(Optional.of(draft));

        Mockito.when(dataverseService.getLatestReleasedOrDeaccessionedVersion(Mockito.any()))
            .thenReturn(Optional.of(previous));

        var step = new StepInvocation("invokeId", "globalId", "datasetId", "1", "1");
        var task = new SetVaultMetadataTask(step, dataverseService);
        task.runTask();
        Mockito.verify(dataverseService).resumeWorkflow(eq(step), argThat(r -> r.getStatus().equals("Success")));
    }

    @Test
    void getMetadata() throws IOException, DataverseException {
        var draft = createDatasetVersion("bagid", "nbn", 1, 1, "DRAFT");
        var previous = createDatasetVersion("bagid", "nbn", 1, 0, "RELEASED");

        Mockito.when(dataverseService.getVersion(Mockito.any(), Mockito.any()))
            .thenReturn(Optional.of(draft));

        Mockito.when(dataverseService.getLatestReleasedOrDeaccessionedVersion(Mockito.any()))
            .thenReturn(Optional.of(previous));

        var step = new StepInvocation("invokeId", "globalId", "datasetId", "1", "1");
        var task = new SetVaultMetadataTask(step, dataverseService);
        var metadata = task.getVaultMetadata(step);

        assertThatMetadataField(metadata, "dansDataversePid").isEqualTo("globalId");
        assertThatMetadataField(metadata, "dansDataversePidVersion").isEqualTo("1.1");
        assertThatMetadataField(metadata, "dansBagId").startsWith("urn:uuid:");
        assertThatMetadataField(metadata, "dansNbn").isEqualTo("nbn");
    }

    @Test
    void getMetadataWithoutPreviousVersion() throws IOException, DataverseException {
        var draft = createDatasetVersion("bagid", "nbn", 1, 1, "DRAFT");

        Mockito.when(dataverseService.getVersion(Mockito.any(), Mockito.any()))
            .thenReturn(Optional.of(draft));

        Mockito.when(dataverseService.getLatestReleasedOrDeaccessionedVersion(Mockito.any()))
            .thenReturn(Optional.empty());

        var step = new StepInvocation("invokeId", "globalId", "datasetId", "1", "1");
        var task = new SetVaultMetadataTask(step, dataverseService);
        var metadata = task.getVaultMetadata(step);

        assertThatMetadataField(metadata, "dansDataversePid").isEqualTo("globalId");
        assertThatMetadataField(metadata, "dansDataversePidVersion").isEqualTo("1.1");
        assertThatMetadataField(metadata, "dansBagId").isEqualTo("bagid");
        assertThatMetadataField(metadata, "dansNbn").isEqualTo("nbn");
    }

    @Test
    void getMetadataWithDifferentPreviousVersion() throws IOException, DataverseException {
        var draft = createDatasetVersion("bagid", "nbn", 1, 1, "DRAFT");
        var previous = createDatasetVersion("different-bagid", "different-nbn", 1, 0, "RELEASED");

        Mockito.when(dataverseService.getVersion(Mockito.any(), Mockito.any()))
            .thenReturn(Optional.of(draft));

        Mockito.when(dataverseService.getLatestReleasedOrDeaccessionedVersion(Mockito.any()))
            .thenReturn(Optional.of(previous));

        var step = new StepInvocation("invokeId", "globalId", "datasetId", "1", "1");
        var task = new SetVaultMetadataTask(step, dataverseService);
        var metadata = task.getVaultMetadata(step);

        assertThatMetadataField(metadata, "dansDataversePid").isEqualTo("globalId");
        assertThatMetadataField(metadata, "dansDataversePidVersion").isEqualTo("1.1");
        assertThatMetadataField(metadata, "dansBagId").isEqualTo("bagid");
        assertThatMetadataField(metadata, "dansNbn").isEqualTo("different-nbn");
    }

    @Test
    void getMetadataWithNullMetadata() throws IOException, DataverseException {
        var draft = createDatasetVersion(null, null, 1, 1, "DRAFT");

        Mockito.when(dataverseService.getVersion(Mockito.any(), Mockito.any()))
            .thenReturn(Optional.of(draft));

        Mockito.when(dataverseService.getLatestReleasedOrDeaccessionedVersion(Mockito.any()))
            .thenReturn(Optional.empty());

        var step = new StepInvocation("invokeId", "globalId", "datasetId", "1", "1");
        var task = new SetVaultMetadataTask(step, dataverseService);
        var metadata = task.getVaultMetadata(step);

        assertThatMetadataField(metadata, "dansDataversePid").isEqualTo("globalId");
        assertThatMetadataField(metadata, "dansDataversePidVersion").isEqualTo("1.1");
        assertThatMetadataField(metadata, "dansBagId").startsWith("urn:uuid:");
        assertThatMetadataField(metadata, "dansNbn").startsWith("urn:nbn:nl:ui:13-");
    }

    @Test
    void getMetadataWithNullMetadataAndPreviousVersion() throws IOException, DataverseException {
        var draft = createDatasetVersion(null, null, 1, 1, "DRAFT");
        var previous = createDatasetVersion("different-bagid", "different-nbn", 1, 0, "RELEASED");

        Mockito.when(dataverseService.getVersion(Mockito.any(), Mockito.any()))
            .thenReturn(Optional.of(draft));

        Mockito.when(dataverseService.getLatestReleasedOrDeaccessionedVersion(Mockito.any()))
            .thenReturn(Optional.of(previous));

        var step = new StepInvocation("invokeId", "globalId", "datasetId", "1", "1");
        var task = new SetVaultMetadataTask(step, dataverseService);
        var metadata = task.getVaultMetadata(step);

        assertThatMetadataField(metadata, "dansDataversePid").isEqualTo("globalId");
        assertThatMetadataField(metadata, "dansDataversePidVersion").isEqualTo("1.1");
        assertThatMetadataField(metadata, "dansBagId").startsWith("urn:uuid:");
        assertThatMetadataField(metadata, "dansNbn").isEqualTo("different-nbn");
    }

    // Helper functions
    private static DatasetVersion createDatasetVersion(String bagId, String nbn, int version, int versionMinor, String versionState) {
        var datasetVersion = new DatasetVersion();
        datasetVersion.setVersionNumber(version);
        datasetVersion.setVersionMinorNumber(versionMinor);
        datasetVersion.setVersionState(versionState);
        datasetVersion.setMetadataBlocks(new HashMap<>());

        var block = new MetadataBlock();
        block.setFields(new ArrayList<>());

        if (bagId != null) {
            block.getFields().add(new PrimitiveSingleValueField("dansBagId", bagId));
        }

        if (nbn != null) {
            block.getFields().add(new PrimitiveSingleValueField("dansNbn", nbn));
        }

        datasetVersion.getMetadataBlocks().put("dansDataVaultMetadata", block);

        return datasetVersion;
    }

    private static AbstractStringAssert<?> assertThatMetadataField(FieldList fieldList, String property) {
        return assertThat(fieldList.getFields())
            .filteredOn("typeName", property)
            .extracting("value")
            .first(as(InstanceOfAssertFactories.STRING));
    }

}

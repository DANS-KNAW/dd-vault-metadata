package nl.knaw.dans.wf.vaultmd.core;

import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;
import nl.knaw.dans.lib.dataverse.model.dataset.FieldList;
import nl.knaw.dans.lib.dataverse.model.workflow.ResumeMessage;
import nl.knaw.dans.wf.vaultmd.api.StepInvocation;

import java.io.IOException;
import java.util.Optional;

public interface DataverseService {

    void resumeWorkflow(StepInvocation stepInvocation, ResumeMessage resumeMessage) throws DataverseException, IOException;

    Optional<DatasetVersion> getVersion(StepInvocation stepInvocation, String name) throws DataverseException, IOException;

    Optional<DatasetVersion> getLatestReleasedOrDeaccessionedVersion(StepInvocation stepInvocation) throws DataverseException, IOException;

    void lockDataset(StepInvocation stepInvocation, String workflow) throws DataverseException, IOException;

    void editMetadata(StepInvocation stepInvocation, FieldList fieldList) throws DataverseException, IOException;
}

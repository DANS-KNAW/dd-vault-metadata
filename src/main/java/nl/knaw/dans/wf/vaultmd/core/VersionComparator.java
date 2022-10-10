package nl.knaw.dans.wf.vaultmd.core;

import nl.knaw.dans.lib.dataverse.model.dataset.DatasetVersion;

import java.util.Comparator;

public class VersionComparator implements Comparator<DatasetVersion> {
    @Override
    public int compare(DatasetVersion left, DatasetVersion right) {
        var leftParts = new int[] { left.getVersionNumber(), left.getVersionMinorNumber() };
        var rightParts = new int[] { right.getVersionNumber(), right.getVersionMinorNumber() };

        for (var i = 0; i < leftParts.length; ++i) {
            var result = Integer.compare(leftParts[i], rightParts[i]);

            if (result != 0) {
                return result;
            }
        }

        return 0;
    }
}

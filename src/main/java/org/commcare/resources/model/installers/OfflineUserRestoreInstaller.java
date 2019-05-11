package org.commcare.resources.model.installers;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.OfflineUserRestore;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Install user restore xml file present in app for use in offline logins.
 * Used for providing a demo user restore.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class OfflineUserRestoreInstaller extends CacheInstaller<OfflineUserRestore> {
    @Override
    protected String getCacheKey() {
        return OfflineUserRestore.STORAGE_KEY;
    }

    @Override
    public boolean initialize(CommCarePlatform platform, boolean isUpgrade) throws
            IOException, InvalidReferenceException, InvalidStructureException,
            XmlPullParserException, UnfullfilledRequirementsException {
        platform.registerDemoUserRestore(storage(platform).read(cacheLocation));
        return true;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return true;
    }

    @Override
    public boolean install(Resource r, ResourceLocation location,
                           Reference ref, ResourceTable table,
                           CommCarePlatform platform, boolean upgrade, boolean recovery)
            throws UnresolvedResourceException, UnfullfilledRequirementsException {
        try {
            OfflineUserRestore offlineUserRestore = OfflineUserRestore.buildInMemoryUserRestore(ref.getStream());
            storage(platform).write(offlineUserRestore);
            if (upgrade) {
                table.commit(r, Resource.RESOURCE_STATUS_INSTALLED);
            } else {
                table.commit(r, Resource.RESOURCE_STATUS_UPGRADE);
            }
            cacheLocation = offlineUserRestore.getID();
        } catch (IOException | XmlPullParserException | InvalidStructureException e) {
            throw new UnresolvedResourceException(r, e.getMessage());
        }
        return true;
    }
}

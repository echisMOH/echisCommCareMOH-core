package org.javarosa.core.services.storage;

import org.javarosa.core.services.Logger;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Manages StorageProviders for JavaRosa, which maintain persistent
 * data on a device.
 *
 * Largely derived from Cell Life's RMSManager
 *
 * @author Clayton Sims
 */
public class StorageManager {

    private final Hashtable<String, IStorageUtilityIndexed> storageRegistry = new Hashtable<>();
    private IStorageIndexedFactory storageFactory;

    public StorageManager(IStorageIndexedFactory factory) {
        setStorageFactory(factory, false);
    }

    /**
     * Attempts to set the storage factory for the current environment and fails and dies if there
     * is already a storage factory set if specified. Should be used by actual applications who need to use
     * a specific storage factory and shouldn't tolerate being pre-empted.
     *
     * @param fact     An available storage factory.
     * @param mustWork true if it is intolerable for another storage factory to have been set. False otherwise
     */
    public void setStorageFactory(IStorageIndexedFactory fact, boolean mustWork) {
        if (storageFactory == null) {
            storageFactory = fact;
        } else {
            if (mustWork) {
                Logger.die("A Storage Factory had already been set when storage factory " + fact.getClass().getName()
                        + " attempted to become the only storage factory", new RuntimeException("Duplicate Storage Factory set"));
            }
        }
    }

    public void registerStorage(String key, Class type) {
        if (storageFactory == null) {
            throw new RuntimeException("No storage factory has been set; I don't know what kind of storage utility to create. Either set a storage factory, or register your StorageUtilitys directly.");
        }

        storageRegistry.put(key, storageFactory.newStorage(key, type));
    }

    public IStorageUtilityIndexed getStorage(String key) {
        if (storageRegistry.containsKey(key)) {
            return storageRegistry.get(key);
        } else {
            throw new RuntimeException("No storage utility has been registered to handle \"" + key + "\"; you must register one first with StorageManager.registerStorage()");
        }
    }

    public void halt() {
        for (Enumeration e = storageRegistry.elements(); e.hasMoreElements(); ) {
            ((IStorageUtilityIndexed)e.nextElement()).close();
        }
    }

    /**
     * Clear all registered elements of storage, including the factory.
     */
    public void forceClear() {
        halt();
        storageRegistry.clear();
        storageFactory = null;
    }
}

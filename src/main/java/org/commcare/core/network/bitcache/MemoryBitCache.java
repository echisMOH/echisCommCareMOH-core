package org.commcare.core.network.bitcache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author ctsims
 */
public class MemoryBitCache implements BitCache {

    private ByteArrayOutputStream bos;
    private byte[] data;

    protected MemoryBitCache() {

    }

    @Override
    public void initializeCache() {
        bos = new ByteArrayOutputStream();
        data = null;
    }

    @Override
    public OutputStream getCacheStream() {
        return bos;
    }

    @Override
    public InputStream retrieveCache() {
        if (data == null) {
            data = bos.toByteArray();
            bos = null;
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    public void release() {
        bos = null;
        data = null;
    }
}

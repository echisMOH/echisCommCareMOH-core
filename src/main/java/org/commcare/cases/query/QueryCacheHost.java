package org.commcare.cases.query;

import java.util.HashMap;

/**
 * A QueryCacheHost is a lifecycle object associated with a particular QueryContext.
 *
 * The goal of a query cache is to establish a lifecycle for data used in potentially very-intense
 * queries which can be terminated once those intense queries are over.
 *
 * Whenever a QueryContxt "escalates" to a new context (which denotes that the new context will be
 * doing a lot of work) a new cache is created for the child context. When optimizations look
 * for cached data they'll find it in the "earliest" context that it was originally requested.
 *
 * This lets nested queries which execute over large datasets maintain their own query caches
 * around the large dataset without injecting that cache data into the original (small N) parent
 * context so that once the large dataset query is over, the small N dataset doesn't keep the
 * memory reserved as it proceeds.
 *
 * Created by ctsims on 1/26/2017.
 */

public class QueryCacheHost {

    HashMap<Class, QueryCache> cacheEntries = new HashMap<>();
    QueryCacheHost parent;

    public QueryCacheHost() {

    }

    public QueryCacheHost(QueryCacheHost parent) {
        this.parent = parent;
    }

    /**
     * Gets a usable copy of the query cache type provided, and if one does not exist,
     * create one at the current cache level.
     */
    public <T extends QueryCache> T getQueryCache(Class<T> cacheType) {
        T t = getQueryCacheOrNull(cacheType);
        if(t != null) {
            return t;
        }
        try {
            t = cacheType.newInstance();
            cacheEntries.put(cacheType, t);
            return t;
        } catch (InstantiationException e) {
            throw new RuntimeException("Couldn't create cache " + cacheType, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't create cache " + cacheType, e);
        }
    }

    /**
     * Get the query cache object provided if one has been created in the current context, or any
     * parent contexts. If not, return null and do not instantiate a new query cache.
     */
    public <T extends QueryCache> T getQueryCacheOrNull(Class<T> cacheType) {
        if (cacheEntries.containsKey(cacheType)) {
            return (T)cacheEntries.get(cacheType);
        } else if (parent != null) {
            return parent.getQueryCacheOrNull(cacheType);
        } else {
            return null;
        }
    }
}

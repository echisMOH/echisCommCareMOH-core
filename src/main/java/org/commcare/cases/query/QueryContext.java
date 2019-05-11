package org.commcare.cases.query;

import org.commcare.cases.query.queryset.CurrentModelQuerySet;
import org.commcare.cases.query.queryset.QuerySetCache;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.trace.EvaluationTrace;

/**
 * A Query Context object is responsible for keeping track of relevant metadata about where a
 * query is executing that may make it possible for the planner to better identify when to
 * trigger certain query handlers.
 *
 * For instance, if an individual query is only looking for one matching case, it may do a single
 * DB read. The context can provide a cue that the query is likely to be run over many other cases,
 * which can provide the planner with a hint to fetch those cases in bulk proactively
 *
 * The QueryContext Object's lifecycle is also used to limit the scope of any of that bulk caching.
 * Since the object lifecycle is paired with the EC of the query, large chunks of memory can be
 * allocated into this object and it will be removed when the context is no longer relevant.
 *
 * Created by ctsims on 1/26/2017.
 */

public class QueryContext {

    /**
     * Tuning parameter to not trigger optimizations for low volume queries.
     *
     * Currently not easy to evaluate this programatically, but should be set at a point which
     * prevents super basic queries from setting up too much overhead.
     */
    public static final int BULK_QUERY_THRESHOLD = 50;

    //TODO: This is a bad reason to keep the EC around here, and locks the lifecycle of this object
    //into the EC
    private EvaluationContext traceRoot;

    private QueryCacheHost cache;

    private QueryContext potentialSpawnedContext;

    /**
     * Context scope roughly keeps track of "how many times is the current query possibly going to
     * run". For instance, when evaluating an xpath like
     *
     * instance('casedb')/casedb/case[@case_type='person'][complex_filter = 'pass']
     *
     * If 500 <case/> nodes match the first predicate (='person') the context scope will escalate
     * to 500. This lets individual expressions later (like 'complex_filter' )identify that it's
     * worth them doing a bit of extra work if they can anticipate making the 'next' evaluation
     * faster.
     */
    private int contextScope = 1;

    public QueryContext() {
        cache = new QueryCacheHost();
    }

    private QueryContext(QueryContext parent) {
        this.traceRoot = parent.traceRoot;
        this.cache = new QueryCacheHost(parent.cache);
        this.contextScope = parent.contextScope;
    }

    /**
     * @param newScope the magnitude of the new query
     * @return either the existing QueryContext or a new (child) QueryContext
     * if the magnitude of the new query exceeds the parent sufficiently
     */
    public QueryContext checkForDerivativeContextAndReturn(int newScope) {
        QueryContext newContext;

        //TODO: I think we may need to clear the spanwed context's spawned context (maybe?) if it
        // was generated
        potentialSpawnedContext = null;
        newContext = new QueryContext(this);
        newContext.contextScope = newScope;

        if (dominates(newContext.contextScope, this.contextScope)) {
            this.reportContextEscalation(newContext, "New");
            return newContext;
        } else {
            return this;
        }
    }

    /**
     * While performing a query, the result of one part of some internal query may be of sufficient
     * scope that even though the current context is small (O(10)), the scope of the internal query
     * may be much, much larger.
     *
     * In those cases an "inline" or temporary context can be spawned for the remainder of the
     * internal evaluation. This may either activate optimizations which would otherwise remain
     * dormant, or provide a new context cache which can be cleared/reclaimed after the internal
     * query finishes.
     *
     * @return either this context or a new query context to be used when evaluating subsequent
     * aspects of a partially completed query.
     */
    public QueryContext testForInlineScopeEscalation(int newScope) {
        if (dominates(newScope, contextScope)) {
            potentialSpawnedContext = new QueryContext(this);
            potentialSpawnedContext.contextScope = newScope;
            reportContextEscalation(potentialSpawnedContext, "Temporary");
            return potentialSpawnedContext;
        } else {
            return this;
        }
    }

    public int getScope() {
        return this.contextScope;
    }

    /**
     * @param newScope the scope of the new query
     * @param existingScope the scope of the existing (parent) query
     * @return Whether the new scope is larger than the current, exceeds the threshold for
     * performing a bulk query, and e
     */
    private boolean dominates(int newScope, int existingScope) {
        return newScope > existingScope &&
                newScope > BULK_QUERY_THRESHOLD &&
                newScope / existingScope > 10;
    }

    private void reportContextEscalation(QueryContext newContext, String label) {
        EvaluationTrace trace = new EvaluationTrace(label + " Query Context [" + newContext.contextScope +"]");
        trace.setOutcome("");
        reportTrace(trace);
    }

    public void reportTrace(EvaluationTrace trace) {
        if (traceRoot != null) {
            traceRoot.reportSubtrace(trace);
        }
    }

    public void setTraceRoot(EvaluationContext traceRoot) {
        this.traceRoot = traceRoot;
    }


    public <T extends QueryCache> T getQueryCache(Class<T> cacheType) {
        return cache.getQueryCache(cacheType);
    }
    public <T extends QueryCache> T getQueryCacheOrNull(Class<T> cacheType) {
        return cache.getQueryCacheOrNull(cacheType);
    }

    public void setHackyOriginalContextBody(CurrentModelQuerySet hackyOriginalContextBody) {
        getQueryCache(QuerySetCache.class).
                addModelQuerySet(CurrentModelQuerySet.CURRENT_QUERY_SET_ID, hackyOriginalContextBody);
    }

    /**
     * Creates a new child context from this base context
     */
    public QueryContext forceNewChildContext() {
        return new QueryContext(this);
    }
}

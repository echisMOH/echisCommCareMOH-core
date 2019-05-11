package org.javarosa.core.model.trace;

import org.javarosa.core.util.OrderedHashtable;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Vector;

/**
 * A Trace Reduction represents a "folded-in" model of an evaluation trace
 * which aggregates stats about multiple traces which followed the same structure
 *
 * Created by ctsims on 1/24/2017.
 */

public class EvaluationTraceReduction extends EvaluationTrace {
    private String expression;

    private int countExecuted = 0;
    private int countRetrievedFromCache = 0;
    private long nanoTime = 0;

    // Maps how many times a given value was computed as the result for the expression that this
    // trace represents
    private final HashMap<String, Integer> valueMap = new HashMap<>();

    // Trace reductions for all of the subtraces of this expression's trace
    private final OrderedHashtable<String, EvaluationTraceReduction> subTraces
            = new OrderedHashtable<>();

    public EvaluationTraceReduction(EvaluationTrace trace) {
        super(trace.getExpression());
        this.expression = trace.getExpression();
        foldIn(trace);
    }

    /**
     * Add the stats about the provided trace to this reduced trace.
     *
     * Assumes that the provided trace represents the same evaluated expression as this trace.
     */
    public void foldIn(EvaluationTrace trace) {
        countExecuted++;
        if (trace.evaluationUsedExpressionCache()) {
            countRetrievedFromCache++;
        }
        nanoTime += trace.getRuntimeInNanoseconds();
        int valueCount = 1;
        if (valueMap.containsKey(trace.getValue())) {
            valueCount = (valueMap.get(trace.getValue()) + 1);
        }
        valueMap.put(trace.getValue(), valueCount);
        Vector<EvaluationTrace> subTraceVector = trace.getSubTraces();
        Vector<EvaluationTrace> copy = (Vector)subTraceVector.clone();
        synchronized (subTraceVector) {
            try {
                for (EvaluationTrace subTrace : copy) {
                    String subKey = subTrace.getExpression();
                    if (subTraces.containsKey(subKey)) {
                        EvaluationTraceReduction reducedSubExpr =
                                subTraces.get(subTrace.getExpression());
                        reducedSubExpr.foldIn(subTrace);
                    } else {
                        EvaluationTraceReduction reducedSubExpr =
                                new EvaluationTraceReduction(subTrace);
                        subTraces.put(subKey, reducedSubExpr);
                    }
                }
            } catch (ConcurrentModificationException cme) {
                throw new RuntimeException(cme);
            }
        }
    }


    public Vector<EvaluationTrace> getSubTraces() {
        return new Vector<EvaluationTrace>(subTraces.values());
    }

    public String getExpression() {
        return expression;
    }

    /**
     * @return The outcome of the expression's execution.
     */
    public String getValue() {
        return String.valueOf(countExecuted);
    }

    protected long getRuntimeInNanoseconds() {
        return nanoTime;
    }


    public String getProfileReport() {
        String response = "{\n";
        response +=  "    time: " + getRuntimeCount(getRuntimeInNanoseconds()) + "\n";
        response +=  "    time/call: " + getRuntimeCount(getRuntimeInNanoseconds() / countExecuted) + "\n";
        int valueResponseCount = 0;
        int totalRecords = valueMap.size();
        for (String key : valueMap.keySet()) {
            response += "    " + key + ": " + valueMap.get(key) + "\n";
            valueResponseCount++;
            if (valueResponseCount >= 10) {
                response += String.format("    ... %s more ...", totalRecords - valueResponseCount);
                break;
            }
        }
        response += "}";
        return response;
    }

    @Override
    public boolean evaluationUsedExpressionCache() {
        return countRetrievedFromCache > 0;
    }

    @Override
    public String getCacheReport() {
        return "{ num times retrieved from cache: " + countRetrievedFromCache + " }";
    }

    public String getRuntimeCount(long l) {
        if (l / 1000 / 1000 > 0) {
            return l /1000 / 1000 + "ms";
        } else if (l / 1000 > 0) {
            return l / 1000 + "us";
        }else {
            return l + "ns";
        }
    }
}

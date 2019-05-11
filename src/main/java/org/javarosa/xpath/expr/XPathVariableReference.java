package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.XPathAnalyzer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class XPathVariableReference extends XPathExpression {
    public XPathQName id;

    @SuppressWarnings("unused")
    public XPathVariableReference() {
    } //for deserialization

    public XPathVariableReference(XPathQName id) {
        this.id = id;
    }

    @Override
    protected Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        return evalContext.getVariable(id.toString());
    }

    @Override
    public String toString() {
        return "{var:" + id.toString() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathVariableReference) {
            XPathVariableReference x = (XPathVariableReference)o;
            return id.equals(x.id);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        id = (XPathQName)ExtUtil.read(in, XPathQName.class, pf);
        cacheState = (CacheableExprState)ExtUtil.read(in, CacheableExprState.class, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, id);
        ExtUtil.write(out, cacheState);
    }

    @Override
    public String toPrettyString() {
        return "$" + id.toString();
    }

    @Override
    public void applyAndPropagateAnalyzer(XPathAnalyzer analyzer) throws AnalysisInvalidException {
        if (analyzer.shortCircuit()) {
            return;
        }
        analyzer.doAnalysis(XPathVariableReference.this);
    }
}

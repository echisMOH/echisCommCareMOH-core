package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Date;

public class XPathNowFunc extends XPathFuncExpr implements VolatileXPathFuncExpr {
    public static final String NAME = "now";
    private static final int EXPECTED_ARG_COUNT = 0;

    public XPathNowFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathNowFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return new Date();
    }

    @Override
    protected boolean rootExpressionTypeIsCacheable() {
        return false;
    }

}

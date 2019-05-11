package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard
public class XPathDateFunc extends XPathFuncExpr {
    public static final String NAME = "date";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathDateFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathDateFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return FunctionUtils.toDate(evaluatedArgs[0]);
    }

}

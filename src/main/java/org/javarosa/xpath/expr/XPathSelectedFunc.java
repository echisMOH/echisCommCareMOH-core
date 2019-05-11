package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard
public class XPathSelectedFunc extends XPathFuncExpr {
    // default to 'selected' but could be 'is-selected'
    // we could also serialize this if we wanted to really preserve it.
    public static final String NAME = "selected";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathSelectedFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSelectedFunc(String name, XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);

        // keep function name from parsing instead of using default
        this.name = name;
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return multiSelected(evaluatedArgs[0], evaluatedArgs[1]);
    }

    /**
     * return whether a particular choice of a multi-select is selected
     *
     * @param o1 XML-serialized answer to multi-select question (i.e, space-delimited choice values)
     * @param o2 choice to look for
     */
    private static Boolean multiSelected(Object o1, Object o2) {

        o1 = FunctionUtils.unpack(o1);
        if (!(o1 instanceof String)) {
            throw generateBadArgumentMessage("selected", 1, "multi-select question", o1);
        }

        o2 = FunctionUtils.unpack(o2);
        if (!(o2 instanceof String)) {
            throw generateBadArgumentMessage("selected", 2, "single potential value from the list of select options", o2);
        }

        String s1 = (String)o1;
        String s2 = ((String)o2).trim();

        return (" " + s1 + " ").contains(" " + s2 + " ");
    }

    private static XPathException generateBadArgumentMessage(String functionName, int argNumber, String type, Object endValue) {
        return new XPathException("Bad argument to function '" + functionName + "'. Argument #" + argNumber + " should be a " + type + ", but instead evaluated to: " + String.valueOf(endValue));
    }


}

package org.javarosa.xpath.analysis;

/**
 * A type of XPathAnalyzer that can evaluate an XPath expression for whether a given condition
 * is true or false for the expression as a whole.
 *
 * Implementing classes should implement doAnalysis() methods for any XPathAnalyzables that are
 * relevant to the condition they are interested in, and those methods should set the value of
 * `result` accordingly.
 *
 * @author Aliza Stone
 */
public abstract class XPathBooleanAnalyzer extends XPathAnalyzer {

    protected boolean result;

    public XPathBooleanAnalyzer() {
        this.result = getDefaultValue();
    }

    protected abstract boolean getDefaultValue();

    protected abstract boolean aggregateResults();

    public boolean computeResult(XPathAnalyzable rootExpression) throws AnalysisInvalidException {
        rootExpression.applyAndPropagateAnalyzer(this);
        return aggregateResults();
    }

    boolean orResults() {
        for (XPathAnalyzer subAnalyzer : this.subAnalyzers) {
            if (((XPathBooleanAnalyzer)subAnalyzer).result) {
                return true;
            }
        }
        return this.result;
    }

    boolean andResults() {
        for (XPathAnalyzer subAnalyzer : this.subAnalyzers) {
            if (!((XPathBooleanAnalyzer)subAnalyzer).result) {
                return false;
            }
        }
        return this.result;
    }
}

package org.javarosa.xpath.analysis.test;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.TreeReferenceAccumulatingAnalyzer;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for accumulating references from XPath Expressions
 *
 * @author Clayton Sims
 */
public class TreeReferenceAccumulatorTest {


    @Test
    public void testNonParses() throws XPathSyntaxException, AnalysisInvalidException {
        runAndTest("4+4");
        runAndTest("if('steve' = 'bob', 4, $jim)");
    }

    @Test
    public void testBasicParses() throws XPathSyntaxException, AnalysisInvalidException {
        runAndTest("instance('casedb')/casedb/case[@case_id = /data/test]/value",
                "instance('casedb')/casedb/case/@case_id",
                "instance('casedb')/casedb/case/value",
                "/data/test");


        runAndTest("if(/data/a + /data/b = '4', /data/a, /data/c)",
                "/data/a",
                "/data/b",
                "/data/c");

        runAndTest("instance('cars')/cars/car[./@make = instance('models')/models/model[@year = '1992']/make]/name",
                "instance('cars')/cars/car/@make",
                "instance('cars')/cars/car/name",
                "instance('models')/models/model/@year",
                "instance('models')/models/model/make");

        runAndTest("instance('cars')/cars/car[../@lead_make = instance('models')/models/model[@year = '1992']/make]/name",
                "instance('cars')/cars/@lead_make",
                "instance('cars')/cars/car/name",
                "instance('models')/models/model/@year",
                "instance('models')/models/model/make");

    }

    @Test
    public void testParsesWithContext() throws XPathSyntaxException, AnalysisInvalidException {
        EvaluationContext ecBase = new EvaluationContext(null);
        EvaluationContext root = new EvaluationContext(ecBase,
                XPathReference.getPathExpr("instance('baseinstance')/base/element").getReference());

        EvaluationContext nestedRoot = new EvaluationContext(root,
                XPathReference.getPathExpr("instance('nestedinstance')/nested/element").getReference());


        runAndTest(root,
                "instance('expr')/ebase/element[@attr = current()/context_value]/value",
                "instance('expr')/ebase/element/value",
                "instance('expr')/ebase/element/@attr",
                "instance('baseinstance')/base/element/context_value");

        runAndTest(nestedRoot,
                "instance('expr')/ebase/element[@attr = current()/context_value]/value",
                "instance('expr')/ebase/element/value",
                "instance('expr')/ebase/element/@attr",
                "instance('nestedinstance')/nested/element/context_value");

        runAndTest(root,
                "../other_element",
                "instance('baseinstance')/base/other_element");


        runAndTest(root,
                "instance('cars')/cars/car[../@lead_make = instance('models')/models/model[@year = current()/year]/make]/name",
                "instance('cars')/cars/@lead_make",
                "instance('cars')/cars/car/name",
                "instance('models')/models/model/@year",
                "instance('models')/models/model/make",
                "instance('baseinstance')/base/element/year"
                );
    }



    @Test
    public void testInvalidReferences() throws XPathSyntaxException {
        runForError("../relativeRef");
        runForError("current()/relative");
    }

    private void runForError(String text) {
        try {
            runAndTest(text);
        } catch (AnalysisInvalidException e) {
            return;
        }
        Assert.fail(String.format("Should have failed to analyse expression %s", text));
    }


    private void runAndTest(String text, String... matches) throws AnalysisInvalidException {
        runAndTest(null, text, matches);
    }

    /**
     * Tests that running TreeReferenceAccumulatingAnalyzer on the XPath expression represented
     * by @text results in accumulating all of the tree refs listed in @matches
     */
    private void runAndTest(EvaluationContext context, String text, String... matches) throws AnalysisInvalidException {
        XPathExpression expression;
        try {
            expression = XPathParseTool.parseXPath(text);
        } catch(XPathSyntaxException e) {
            throw new RuntimeException(e);
        }

        TreeReferenceAccumulatingAnalyzer analyzer;
        if (context == null) {
            analyzer = new TreeReferenceAccumulatingAnalyzer();
        } else {
            analyzer = new TreeReferenceAccumulatingAnalyzer(context);
        }

        Set<TreeReference> results = analyzer.accumulate(expression);
        if (results == null) {
            throw new AnalysisInvalidException(String.format("Couldn't analyze expression: %s",text));
        }

        Set<TreeReference> expressions = new HashSet<>();

        for (String match : matches) {
            expressions.add(XPathReference.getPathExpr(match).getReference());
        }

        Assert.assertEquals(text, expressions, results);
    }
}
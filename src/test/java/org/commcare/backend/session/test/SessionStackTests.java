package org.commcare.backend.session.test;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.Action;
import org.commcare.suite.model.EntityDatum;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.test.utilities.MockApp;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.test_utils.ExprEvalUtils;
import org.javarosa.xpath.XPathMissingInstanceException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import org.commcare.session.SessionFrame;
import org.junit.Test;

import java.io.InputStream;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * This is a super basic test just to make sure the test infrastructure is working correctly
 * and to act as an example of how to build template app tests.
 *
 * Created by ctsims on 8/14/2015.
 */
public class SessionStackTests {

    @Test
    public void testDoubleManagementAndOverlappingStack() throws Exception {
        MockApp mockApp = new MockApp("/complex_stack/");
        SessionWrapper session = mockApp.getSession();

        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("m0");

        assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());

        session.setComputedDatum();

        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        EntityDatum entityDatum = (EntityDatum)session.getNeededDatum();
        assertEquals("case_id", entityDatum.getDataId());

        Vector<Action> actions = session.getDetail(entityDatum.getShortDetail()).getCustomActions(session.getEvaluationContext());

        if (actions == null || actions.isEmpty()) {
            fail("Detail screen stack action was missing from app!");
        }
        Action dblManagement = actions.firstElement();

        session.executeStackOperations(dblManagement.getStackOperations(), session.getEvaluationContext());

        if (session.getNeededData() != null) {
            fail("After executing stack frame steps, session should be redirected");
        }

        assertEquals("http://commcarehq.org/test/placeholder_destination", session.getForm());

        EvaluationContext ec = session.getEvaluationContext();

        CaseTestUtils.xpathEvalAndCompare(ec, "count(instance('session')/session/data/calculated_data)", 1);

        CaseTestUtils.xpathEvalAndCompare(ec, "instance('session')/session/data/calculated_data", "new");
    }

    @Test
    public void testViewNav() throws Exception {
        MockApp mockApp = new MockApp("/complex_stack/");
        SessionWrapper session = mockApp.getSession();

        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("m3-f0");

        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());

        assertEquals("case_id_to_send", session.getNeededDatum().getDataId());

        assertFalse("Session incorrectly determined a view command", session.isViewCommand(session.getCommand()));

        session.setDatum("case_id_to_send", "case_one");

        session.finishExecuteAndPop(session.getEvaluationContext());

        assertEquals("m2", session.getCommand());

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/case_id", "case_one");

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "count(instance('session')/session/data/case_id_to_send)", "0");

        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
    }

    @Test
    public void testViewNonNav() throws Exception {
        MockApp mockApp = new MockApp("/complex_stack/");
        SessionWrapper session = mockApp.getSession();

        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("m4-f0");

        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());

        assertEquals("case_id_to_view", session.getNeededDatum().getDataId());

        assertTrue("Session incorrectly tagged a view command", session.isViewCommand(session.getCommand()));
    }

    @Test
    public void testOutOfOrderStack() throws Exception {
        MockApp mockApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mockApp.getSession();

        // Select a form that has 3 datum requirements to enter (in order from suite.xml: case_id,
        // case_id_new_visit_0, usercase_id)
        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0");

        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0-f3");

        // Set 2 of the 3 needed datums, but not in order (1st and 3rd)
        session.setDatum("case_id", "case_id_value");
        session.setDatum("usercase_id", "usercase_id_value");

        // Session should now need the case_id_new_visit_0, which is a computed datum
        assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());

        // The key of the needed datum should be "case_id_new_visit_0"
        assertEquals("case_id_new_visit_0", session.getNeededDatum().getDataId());

        // Add the needed datum to the stack and confirm that the session is now ready to proceed
        session.setDatum("case_id_new_visit_0", "visit_id_value");
        assertEquals(null, session.getNeededData());
    }

    @Test
    public void testOutOfOrderStackComplex() throws Exception {
        MockApp mockApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mockApp.getSession();

        // Select a form that has 3 datum requirements to enter (in order from suite.xml: case_id,
        // case_id_new_visit_0, usercase_id)
        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0");

        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0-f3");

        // Set 2 of the 3 needed datums, so that the datum that is actually still needed (case_id)
        // is NOT a computed value, but the "last" needed datum is a computed value
        session.setDatum("case_id_new_visit_0", "visit_id_value");
        session.setDatum("usercase_id", "usercase_id_value");

        // Session should now see that it needs a normal datum val (NOT a computed val)
        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());

        // The key of the needed datum should be "case_id"
        assertEquals("case_id", session.getNeededDatum().getDataId());

        // Add the needed datum to the stack and confirm that the session is now ready to proceed
        session.setDatum("case_id", "case_id_value");
        assertEquals(null, session.getNeededData());
    }

    @Test
    public void testUnnecessaryDataOnStack() throws Exception {
        MockApp mockApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mockApp.getSession();

        // Select a form that has 3 datum requirements to enter (in order from suite.xml: case_id,
        // case_id_new_visit_0, usercase_id)
        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0");

        assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0-f3");

        // Put a bunch of random data on the stack such that there are more datums on the stack
        // than the total number of needed datums for this session (which is 3)
        session.setDatum("random_id_1", "random_val_1");
        session.setDatum("random_id_2", "random_val_2");
        session.setDatum("random_id_3", "random_val_3");
        session.setDatum("random_id_4", "random_val_4");

        // Now go through and check that the session effectively ignores the rubbish on the stack
        // and still sees itself as needing each of the datums defined for this form, in the correct
        // order

        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        assertEquals("case_id", session.getNeededDatum().getDataId());

        session.setDatum("case_id", "case_id_value");
        assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());
        assertEquals("case_id_new_visit_0", session.getNeededDatum().getDataId());

        session.setDatum("case_id_new_visit_0", "visit_id_value");
        assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());
        assertEquals("usercase_id", session.getNeededDatum().getDataId());

        session.setDatum("usercase_id", "usercase_id_value");
        assertEquals(null, session.getNeededData());
    }

    /**
     * Test that instance stored on the session stack (from remote query
     * results), that isn't supposed to adhere to the casedb xml template,
     * doesn't in fact adhere to it
     */
    @Test
    public void testNonCaseInstanceOnStack() throws Exception {
        MockApp mockApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mockApp.getSession();

        session.setCommand("patient-noncase-search");
        assertEquals(session.getNeededData(), SessionFrame.STATE_QUERY_REQUEST);

        ExternalDataInstance dataInstance =
                buildRemoteExternalDataInstance(this.getClass(), session,
                        "/session-tests-template/patient_query_result.xml");
        session.setQueryDatum(dataInstance);

        ExprEvalUtils.testEval("instance('patients')/patients/patient[@id = '321']/name",
                session.getEvaluationContext(),
                "calbert");
    }

    /**
     * Test that instances stored on the session stack (from remote query
     * results), adheres to the casedb xml template as expected from the query
     * having the template="case" attribute Also ensure the instance is
     * correctly popped off with the associated frame step
     */
    @Test
    public void testCaseInstancesOnStack() throws Exception {
        MockApp mockApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mockApp.getSession();

        session.setCommand("patient-case-search");
        assertEquals(session.getNeededData(), SessionFrame.STATE_QUERY_REQUEST);

        ExternalDataInstance dataInstance =
                buildRemoteExternalDataInstance(this.getClass(), session,
                        "/session-tests-template/patient_query_result.xml");
        session.setQueryDatum(dataInstance);

        ExprEvalUtils.testEval("instance('patients')/patients/case[@id = '123']/name",
                session.getEvaluationContext(),
                "bolivar");

        // demonstrate that paths that aren't 'casedb/case/...' fail
        ExprEvalUtils.testEval("instance('patients')/patients/patient[@id = '321']/name",
                session.getEvaluationContext(),
                new XPathTypeMismatchException());

        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        assertEquals("case_id", session.getNeededDatum().getDataId());
        session.setDatum("case_id", "case_id_value");

        session.stepBack();
        ExprEvalUtils.testEval("instance('patients')/patients/case[@id = '123']/name",
                session.getEvaluationContext(),
                "bolivar");

        session.stepBack();
        assertInstanceMissing(session, "instance('patients')/patients/case/bolivar");

        session.setQueryDatum(dataInstance);
        ExprEvalUtils.testEval("instance('patients')/patients/case[@id = '123']/name",
                session.getEvaluationContext(),
                "bolivar");

        session.finishExecuteAndPop(session.getEvaluationContext());
        assertInstanceMissing(session, "instance('patients')/patients/case/bolivar");
        ExprEvalUtils.testEval("instance('session')/session/data/case_id",
                session.getEvaluationContext(),
                "bolivar");
    }

    @Test
    public void testActionParsing() throws Exception {
        MockApp mApp = new MockApp("/complex_stack/");
        SessionWrapper session = mApp.getSession();

        session.setCommand("test-actions");

        assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        EntityDatum entityDatum = (EntityDatum)session.getNeededDatum();
        assertEquals("case_id", entityDatum.getDataId());

        EvaluationContext ec = session.getEvaluationContext();
        Vector<Action> actions = session.getDetail(entityDatum.getShortDetail()).getCustomActions(ec);

        // Only 2 of the 3 actions should be returned, because 1 has a relevant condition of false()
        assertEquals(2, actions.size());

        Action actionToInspect = actions.get(1);
        assertTrue(actionToInspect.hasActionBarIcon());
        assertEquals("Jump to Menu 2 Form 1", actionToInspect.getDisplay().getText().evaluate(ec));
        assertEquals(1, actionToInspect.getStackOperations().size());
    }

    private static void assertInstanceMissing(SessionWrapper session, String xpath)
            throws XPathSyntaxException {
        try {
            ExprEvalUtils.xpathEval(session.getEvaluationContext(), xpath);
            fail("instance('patients') should not be available");
        } catch (XPathMissingInstanceException e) {
            // expected
        }
    }

    /**
     * Make sure that stepping backwards before doing anything else doesn't crash
     */
    @Test
    public void testStepBackAtBase() throws Exception {
        MockApp mockApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mockApp.getSession();
        session.stepBack();

    }

    static ExternalDataInstance buildRemoteExternalDataInstance(Class cls,
                                                                SessionWrapper sessionWrapper,
                                                                String resourcePath) {
        RemoteQuerySessionManager remoteQuerySessionManager =
                RemoteQuerySessionManager.buildQuerySessionManager(sessionWrapper,
                        sessionWrapper.getEvaluationContext());
        InputStream is = cls.getResourceAsStream(resourcePath);
        Pair<ExternalDataInstance, String> instanceOrError =
                remoteQuerySessionManager.buildExternalDataInstance(is);
        assertNotNull(instanceOrError.first);
        return instanceOrError.first;
    }
}

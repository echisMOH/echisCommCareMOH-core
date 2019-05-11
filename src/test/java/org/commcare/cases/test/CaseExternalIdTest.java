package org.commcare.cases.test;

import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.test.utilities.TestProfileConfiguration;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

/**
 * Tests verifying that the @external_id index performs correctly
 *
 * @author wpride
 */
@RunWith(value = Parameterized.class)
public class CaseExternalIdTest {

    private MockUserDataSandbox sandbox;

    private TestProfileConfiguration config;
    @Parameterized.Parameters(name = "{0}")
    public static Collection data() {
        return TestProfileConfiguration.BulkOffOn();
    }

    public CaseExternalIdTest(TestProfileConfiguration config) {
        this.config = config;
    }

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test
    public void testReadExternalId() throws Exception {
        config.parseIntoSandbox(this.getClass().getResourceAsStream("/case_create_external_id.xml"), sandbox, false);
        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(this.sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@external_id = '123']/case_name", "Two"));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@external_id = '123' and true()]/case_name", "Two"));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'case_two']/@external_id", "123"));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'case_two'][@external_id = '123']/case_name", "Two"));
    }

    @Test
    public void testNoExternalIdFails() throws Exception {
        config.parseIntoSandbox(this.getClass().getResourceAsStream("/case_create_basic.xml"), sandbox, false);
        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(this.sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@external_id = '123']/case_name", ""));
    }

    @Test
    public void testEmptyDbWorks() throws Exception {
        config.parseIntoSandbox(this.getClass().getResourceAsStream("/empty_restore.xml"), sandbox, false);
        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(this.sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec,
                "instance('casedb')/casedb/case[@case_id = '123']/@external_id", ""));
    }
}

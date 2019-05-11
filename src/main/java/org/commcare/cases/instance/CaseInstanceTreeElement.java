package org.commcare.cases.instance;

import org.commcare.cases.model.Case;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;

/**
 * The root element for the <casedb> abstract type. All children are
 * nodes in the case database. Depending on instantiation, the <casedb>
 * may include only a subset of the full db.
 *
 * @author ctsims
 */
public class CaseInstanceTreeElement extends StorageInstanceTreeElement<Case, CaseChildElement> {

    public static final String MODEL_NAME = "casedb";

    //Xpath parsing is sllllllloooooooowwwwwww
    public final static XPathPathExpr CASE_ID_EXPR = XPathReference.getPathExpr("@case_id");
    public final static XPathPathExpr CASE_ID_EXPR_TWO = XPathReference.getPathExpr("./@case_id");
    private final static XPathPathExpr CASE_TYPE_EXPR = XPathReference.getPathExpr("@case_type");
    private final static XPathPathExpr CASE_STATUS_EXPR = XPathReference.getPathExpr("@status");
    private final static XPathPathExpr CASE_INDEX_EXPR = XPathReference.getPathExpr("index/*");
    private final static XPathPathExpr OWNER_ID_EXPR = XPathReference.getPathExpr("@owner_id");
    private final static XPathPathExpr EXTERNAL_ID_EXPR = XPathReference.getPathExpr("@external_id");

    public CaseInstanceTreeElement(AbstractTreeElement instanceRoot,
                                   IStorageUtilityIndexed<Case> storage) {
        super(instanceRoot, storage, MODEL_NAME, "case");
    }

    @Override
    protected CaseChildElement buildElement(StorageInstanceTreeElement<Case, CaseChildElement> storageInstance,
                                            int recordId, String id, int mult) {
        return new CaseChildElement(storageInstance, recordId, null, mult);
    }

    @Override
    protected CaseChildElement getChildTemplate() {
        return CaseChildElement.buildCaseChildTemplate(this);
    }

    @Override
    protected String translateFilterExpr(XPathPathExpr expressionTemplate, XPathPathExpr matchingExpr,
                                         Hashtable<XPathPathExpr, String> indices) {
        String filter = super.translateFilterExpr(expressionTemplate, matchingExpr, indices);

        //If we're matching a case index, we've got some magic to take care of. First,
        //generate the expected case ID
        if (expressionTemplate == CASE_INDEX_EXPR) {
            filter += matchingExpr.steps[1].name.name;
        }

        return filter;
    }

    @Override
    protected Hashtable<XPathPathExpr, String> getStorageIndexMap() {
        Hashtable<XPathPathExpr, String> indices = new Hashtable<>();

        //TODO: Much better matching
        indices.put(CASE_ID_EXPR, Case.INDEX_CASE_ID);
        indices.put(CASE_ID_EXPR_TWO, Case.INDEX_CASE_ID);
        indices.put(CASE_TYPE_EXPR, Case.INDEX_CASE_TYPE);
        indices.put(CASE_STATUS_EXPR, Case.INDEX_CASE_STATUS);
        indices.put(CASE_INDEX_EXPR, Case.INDEX_CASE_INDEX_PRE);
        indices.put(OWNER_ID_EXPR, Case.INDEX_OWNER_ID);
        indices.put(EXTERNAL_ID_EXPR, Case.INDEX_EXTERNAL_ID);

        return indices;
    }

    public String getStorageCacheName() {
        return CaseInstanceTreeElement.MODEL_NAME;
    }
}

package org.commcare.cases.instance;

import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.FormLoadingUtils;
import org.javarosa.xml.util.InvalidStructureException;

import java.io.IOException;

/**
 * An external data instance that respects CaseDB template specifications.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CaseDataInstance extends ExternalDataInstance {
    private static TreeElement caseDbSpecTemplate = null;
    private static final String CASEDB_WILD_CARD = "CASEDB_WILD_CARD";

    public CaseDataInstance() {
        // For serialization
    }

    /**
     * Copy constructor
     */
    public CaseDataInstance(ExternalDataInstance instance) {
        super(instance);
    }

    /**
     * Does the reference follow the statically defined CaseDB spec?
     */
    @Override
    public boolean hasTemplatePath(TreeReference ref) {
        loadTemplateSpecLazily();


        // enforce (artificial) constraint that the instance name matches the
        // root element name. For instance, instance('casedb')/casedb
        boolean instanceNameMatch = ref.size() > 0 && instanceid.equals(ref.getName(0));

        return instanceNameMatch && followsTemplateSpec(ref, caseDbSpecTemplate, 1);
    }

    private static synchronized void loadTemplateSpecLazily() {
        final String errorMsg = "Failed to load casedb template spec xml file " +
                "while checking if case related xpath follows the template structure.";
        if (caseDbSpecTemplate == null) {
            try {
                caseDbSpecTemplate =
                        FormLoadingUtils.xmlToTreeElement("/casedb_instance_structure.xml");
            } catch (InvalidStructureException | IOException e) {
                throw new RuntimeException(errorMsg);
            }
        }
    }

    private static boolean followsTemplateSpec(TreeReference refToCheck,
                                               TreeElement currTemplateNode,
                                               int currRefDepth) {
        if (currTemplateNode == null) {
            return false;
        }

        if (currRefDepth == refToCheck.size()) {
            return true;
        }

        String name = refToCheck.getName(currRefDepth);

        if (refToCheck.getMultiplicity(currRefDepth) == TreeReference.INDEX_ATTRIBUTE) {
            TreeElement templateAttr = currTemplateNode.getAttribute(null, name);
            return followsTemplateSpec(refToCheck, templateAttr, currRefDepth + 1);
        } else {
            TreeElement nextTemplateNode = currTemplateNode.getChild(name, 0);
            if (nextTemplateNode == null) {
                // didn't find a node of the given name in the template, check
                // if a wild card exists at this level of the template.
                nextTemplateNode = currTemplateNode.getChild(CASEDB_WILD_CARD, 0);
            }
            return followsTemplateSpec(refToCheck, nextTemplateNode, currRefDepth + 1);
        }
    }

    @Override
    public boolean useCaseTemplate() {
        return true;
    }
}

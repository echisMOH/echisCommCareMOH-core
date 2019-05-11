package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Application callout described in suite.xml
 * Used in callouts from EntitySelectActivity and EntityDetailActivity
 *
 * @author wpride1 on 4/14/15.
 */
public class Callout implements Externalizable, DetailTemplate {
    private String actionName;
    private String image;
    private String displayName;
    private String type;
    private Hashtable<String, String> extras;
    private Vector<String> responses;
    private boolean isAutoLaunching;
    private boolean assumePlainTextValues;

    private static final String KEY_FORCE_XPATH_PARSING = "force_xpath_parsing";
    private static final String KEY_FORCE_XPATH_PARSING_VALUE_TRUE = "yes";

    /**
     * Allows case list intent callouts to map result data to cases. 'header'
     * is the column header text and 'template' is the key used for mapping a
     * callout result data point to a case, should usually be the case id.
     */
    private DetailField responseDetail;

    /**
     * For externalization
     */
    public Callout() {

    }

    public Callout(String actionName, String image, String displayName,
                   Hashtable<String, String> extras, Vector<String> responses,
                   DetailField responseDetail, String type, boolean isAutoLaunching) {
        this.actionName = actionName;
        this.image = image;
        this.displayName = displayName;
        this.extras = extras;
        this.responses = responses;
        this.responseDetail = responseDetail;
        this.type = type;
        this.isAutoLaunching = isAutoLaunching;
        this.assumePlainTextValues = false;
    }

    @Override
    public CalloutData evaluate(EvaluationContext context) {
        Hashtable<String, String> evaluatedExtras = new Hashtable<>();
        boolean forceXpathParsing = forceXpathParsing();
        Enumeration keys = extras.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            if (!key.contentEquals(KEY_FORCE_XPATH_PARSING)) {
                String rawValue = extras.get(key);
                if (assumePlainTextValues && !forceXpathParsing) {
                    evaluatedExtras.put(key, rawValue);
                } else {
                    try {
                        String evaluatedValue =
                                FunctionUtils.toString(XPathParseTool.parseXPath(rawValue).eval(context));
                        evaluatedExtras.put(key, evaluatedValue);
                    } catch (XPathSyntaxException e) {
                        // do nothing
                    }
                }
            }
        }

        // emit a CalloutData with the extras evaluated. used for the detail screen.
        return new CalloutData(actionName, image, displayName, evaluatedExtras, responses, type);
    }

    // Returns true if force_xpath_parsing is yes
    private boolean forceXpathParsing() {
        Enumeration keys = extras.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            if (key.contentEquals(KEY_FORCE_XPATH_PARSING)) {
                String forceXpathVal = extras.get(key);
                return forceXpathVal.contentEquals(KEY_FORCE_XPATH_PARSING_VALUE_TRUE);
            }
        }
        return false;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        displayName = ExtUtil.readString(in);
        actionName = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        image = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        extras = (Hashtable<String, String>)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
        responses = (Vector<String>)ExtUtil.read(in, new ExtWrapList(String.class), pf);
        responseDetail = (DetailField)ExtUtil.read(in, new ExtWrapNullable(DetailField.class), pf);
        type = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
        isAutoLaunching = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, displayName);
        ExtUtil.write(out, new ExtWrapNullable(actionName));
        ExtUtil.write(out, new ExtWrapNullable(image));
        ExtUtil.write(out, new ExtWrapMap(extras));
        ExtUtil.write(out, new ExtWrapList(responses));
        ExtUtil.write(out, new ExtWrapNullable(responseDetail));
        ExtUtil.write(out, new ExtWrapNullable(type));
        ExtUtil.writeBool(out, isAutoLaunching);
    }

    public String getImage() {
        return image;
    }

    public String getActionName() {
        return actionName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Hashtable<String, String> getExtras() {
        return extras;
    }

    public Vector<String> getResponses() {
        return responses;
    }

    public DetailField getResponseDetailField() {
        return responseDetail;
    }

    public boolean isAutoLaunching() {
        return isAutoLaunching;
    }

    public boolean isSimprintsCallout() {
        return "com.simprints.id.IDENTIFY".equals(actionName);
    }

    public void setAssumePlainTextValues() {
        this.assumePlainTextValues = true;
    }
}

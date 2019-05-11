package org.javarosa.engine;

import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.engine.xml.XmlUtil;
import org.javarosa.model.xform.DataModelSerializer;
import org.javarosa.xpath.XPathLazyNodeset;
import org.javarosa.xpath.expr.FunctionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * Custom functions to be used in form player debug tools.
 *
 * Allows users to:
 * - override 'today()' and 'now()' with custom dates.
 * - perform introspection on xpath references with 'print()'
 * - override 'here()' with custom location.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FunctionExtensions {
    public static class TodayFunc implements IFunctionHandler {
        private final String name;
        private final Date date;

        public TodayFunc(String name, Date date) {
            this.name = name;
            this.date = date;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Vector getPrototypes() {
            Vector<Class[]> p = new Vector<>();
            p.addElement(new Class[0]);
            return p;
        }

        @Override
        public boolean rawArgs() {
            return false;
        }

        @Override
        public Object eval(Object[] args, EvaluationContext ec) {
            if (date != null) {
                return date;
            } else {
                return new Date();
            }
        }
    }

    static class PrintFunc implements IFunctionHandler {
        private final InstanceInitializationFactory iif;

        protected PrintFunc(InstanceInitializationFactory iif) {
            this.iif = iif;
        }

        @Override
        public String getName() {
            return "print";
        }

        @Override
        public Vector getPrototypes() {
            Vector<Class[]> p = new Vector<>();
            p.addElement(new Class[0]);
            return p;
        }

        @Override
        public boolean rawArgs() {
            return true;
        }

        @Override
        public Object eval(Object[] args, EvaluationContext ec) {
            Vector<TreeReference> refs = ((XPathLazyNodeset)args[0]).getReferences();

            for (TreeReference ref : refs) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataModelSerializer serializer;
                try {
                    serializer = new DataModelSerializer(bos, iif);
                    serializer.serialize(ec.resolveReference(ref));
                    System.out.println(XmlUtil.getPrettyXml(bos.toByteArray()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return "";
        }
    }

    static class DocFunc implements IFunctionHandler {
        private static JSONObject funcSpec = null;

        @Override
        public String getName() {
            return "doc";
        }

        @Override
        public Vector getPrototypes() {
            Vector<Class[]> p = new Vector<>();
            p.addElement(new Class[0]);
            return p;
        }

        @Override
        public boolean rawArgs() {
            return true;
        }

        @Override
        public Object eval(Object[] args, EvaluationContext ec) {
            String functionName = (String)args[0];

            Class functionClass = FunctionUtils.getXPathFuncListMap().get(functionName);
            if (functionClass == null) {
                return "Function '" + functionName + "' doesn't exist";
            }

            loadFunctionSpec();

            JSONObject spec = funcSpec.getJSONObject(functionName);
            if (spec == null) {
                return "Function '" + functionName + "' doesn't exist in spec";
            } else {
                return spec.getString("doc");
            }
        }

        private static void loadFunctionSpec() {
            if (funcSpec == null) {
                try {
                    InputStream is = FunctionExtensions.class.getResourceAsStream("/xpath_function_spec.json");
                    byte[] manifest = StreamsUtil.inputStreamToByteArray(is);
                    funcSpec = new JSONObject(new String(manifest));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class ListXPathFunc implements IFunctionHandler {

        @Override
        public String getName() {
            return "funcs";
        }

        @Override
        public Vector getPrototypes() {
            Vector<Class[]> p = new Vector<>();
            p.addElement(new Class[0]);
            return p;
        }

        @Override
        public boolean rawArgs() {
            return true;
        }

        @Override
        public Object eval(Object[] args, EvaluationContext ec) {
            StringBuilder builder = new StringBuilder();
            List<String> sortedFunctionNames = FunctionUtils.xPathFuncList();
            Collections.sort(sortedFunctionNames);
            for (String funcName : sortedFunctionNames) {
                builder.append(funcName).append("\n");
            }
            return builder.toString();
        }
    }
}

package org.commcare.core.graph.suite;

import org.commcare.suite.model.Text;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Definition of an annotation, which is text drawn at a specified x, y coordinate on a graph.
 *
 * @author jschweers
 */
public class Annotation implements Externalizable {
    private Text mX;
    private Text mY;
    private Text mAnnotation;

    public Annotation() {

    }

    public Annotation(Text x, Text y, Text annotation) {
        mX = x;
        mY = y;
        mAnnotation = annotation;
    }

    public Text getX() {
        return mX;
    }

    public Text getY() {
        return mY;
    }

    public Text getAnnotation() {
        return mAnnotation;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        mX = (Text)ExtUtil.read(in, Text.class, pf);
        mY = (Text)ExtUtil.read(in, Text.class, pf);
        mAnnotation = (Text)ExtUtil.read(in, Text.class, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, mX);
        ExtUtil.write(out, mY);
        ExtUtil.write(out, mAnnotation);
    }
}
